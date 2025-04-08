def call(Map config) {
    pipeline {
        agent { label 'python_agent' }
        triggers {
            githubPush()
        }
        stages {
            stage('Lint') {
                steps {
                    dir("${config.serviceDir}") {
                        sh 'pylint *.py --fail-under=5'
                    }
                }
            }

            stage('Security') {
                steps {
                    dir("${config.serviceDir}") {
                        sh 'curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin'
                        sh "docker build -t ${config.imageName}:temp ."
                        sh "trivy image --exit-code 1 --severity HIGH,CRITICAL ${config.imageName}:temp"
                    }
                }
            }



            
            stage('Package') {
                when {
                    expression { env.GIT_BRANCH == 'origin/main' }
                }
                steps {
                    dir("${config.serviceDir}") {
                        withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) {
                            sh "docker login -u 'sim44' -p '$TOKEN' docker.io"
                            sh "docker build -t ${config.imageName}:latest --tag ${config.imageName}:${config.tag} ."
                            sh "docker push ${config.imageName}:${config.tag}"
                        }
                    }
                }
            }

            stage('Deploy') {
              when {
                expression { return params.DEPLOY == true }
              }
              steps {
                sshagent(['ssh-to-3855vm']) {
                 sh '''
                  python3 -m venv venv
                  ./venv/bin/pip install ansible
                  ANSIBLE_HOST_KEY_CHECKING=False ./venv/bin/ansible-playbook -i /home/azureuser/ansible/inventory.yml /home/azureuser/ansible/deploy_project.yml
                '''
                }
              }
            }

        }

        parameters {
            booleanParam(name: 'DEPLOY', defaultValue: false, description: 'Trigger Deploy Stage Manually')
        }
    }
}
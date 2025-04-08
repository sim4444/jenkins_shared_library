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


            // stage('Debug Docker Access') {
            //     steps {
            //         dir("${config.serviceDir}") {
            //         sh 'echo "Current User: $(whoami)"'
            //         sh 'echo "Group Membership:" && id'
            //         sh 'echo "Docker Access Test:" && docker ps'
            //         }
            //     }
            // }

            stage('Security') {
                steps {
                    dir("${config.serviceDir}") {
                        sh 'python3 -m venv venv'
                        sh './venv/bin/pip install bandit'
                        sh './venv/bin/bandit -r . -x ./venv -lll -iii -s MEDIUM'
                    }
                }
            }



            
            stage('Deploy') {
                when {
                    expression { return params.DEPLOY == true }
                }
                steps {
                    sshagent(['ssh-to-3855vm1']) {
                        sh '''
                            python3 -m venv venv
                            ./venv/bin/pip install ansible
                            ANSIBLE_HOST_KEY_CHECKING=False ./venv/bin/ansible-playbook -i ansible/inventory.ini ansible/playbook.yml
                        '''
                    }
                }
            }




            stage('Deploy') {
              when {
                expression { return params.DEPLOY == true }
              }
              steps {
                sshagent(['ssh-to-3855vm1']) {
                 sh '''
                  python3 -m venv venv
                  ./venv/bin/pip install ansible
                  ANSIBLE_HOST_KEY_CHECKING=False ./venv/bin/ansible-playbook -i /home/azureuser/ansible/inventory.ini /home/azureuser/ansible/playbook.yml
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
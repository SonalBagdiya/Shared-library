 def call(Map buildopts = [:]) {
     pipeline {
    agent none

    stages {
        stage('Compile Application') {
            agent {
                kubernetes {
		            label "Jenkins-${env.JOB_NAME}"
                    yaml libraryResource('agents/nodejs-slave.yaml')
                }
            }

            steps {
                container('nodejs') {
                    script {
			    echo 'Testing..'
                            sh 'ls -lart /'
                            sh 'npm --version'
                            sh 'node --version'
                            sh 'npm install'
                            sh 'npm run export'
			   }
                        }
                    }
                }
            }    
        }
     
    }
 def call(Map buildopts = [:]) {
     pipeline {
    agent none

    stages {
        stage('Compile Application') {
            agent {
                kubernetes {
		    label "Jenkins-${env.JOB_NAME}"
                    yaml libraryResource('agents/java-maven-slave.yaml')
                }
            }

            steps {
                container('jnlp') {
                    script {
			 sh 'mvn -version'
                         sh "echo testing"
                        }
                    }
                }
            }    
        }
     
    }
 }
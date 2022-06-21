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
				            echo "testing"
			                // configFileProvider([configFile(fileId: 'custom-maven-settings', variable: 'MAVEN_SETTINGS_XML')]) {
			               //  sh 'mvn -version'
                           //  sh "source /usr/local/bin/scl_enable && mvn -s ${MAVEN_SETTINGS_XML} -version"
			                }
                        }
                    }
                }
            }    
        }
     
    }
 
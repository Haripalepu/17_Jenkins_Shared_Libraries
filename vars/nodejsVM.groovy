//This pipeline works for any Node js language project & which is going to deploy the application in VM.
//We have to call this file from 17_Jenkins_Catalogue/Jenkinsfile.
//We have to add this git repo in jenkins system configurations then only jenkinfiles can call this.

def call(Map configMap){  //call is a function 
pipeline {
    agent {
        node {
            label 'Agent' //Name of the Agent label 
        }
    }

    environment { 
        packageVersion = ''
        nexusURL = '172.31.81.162:8081' //Mention your Nexus Url
    }

    options {
        timeout(time: 1, unit: 'HOURS')
        disableConcurrentBuilds()  //It won't allow us to run two builds at a time.
    }

    parameters {
        booleanParam(name: 'Deploy', defaultValue: false, description: 'Toggle this value')
    }

    // build
    stages {
        stage('Get the version') { 
            steps {
                script { //Groovy scripting. def command is used declare a variable in pipeline.
                    def packageJson = readJSON file: 'package.json' //Now we will get all data from the package.json file and stores in packageJson by using Pipeline Utility Steps plugin.
                    packageVersion = packageJson.version //Now from that file we can take the version id. 
                    echo "application version: $packageVersion" //Calling from line no 12
                }
            }
        }
        stage('Install dependencies') {
            steps {     //Shell commands in pipeline. To run the below command Node Js should be installed in agent.
                sh """   
                    npm install  
                """
            }
        }
        stage('Unit Testing') {
            steps {     
                sh """   
                    echo "Unit testing will run here"  
                """
            }
        }
        stage('Sonar Scanning') { //The below command will read the sonar-project.properties file then it will automatically scans the code and uploads the results to sonarqube console.
            steps {     
                sh """   
                    sonar-scanner  
                """
            }
        }
        stage('Build the zip file') {
            steps {   // zip is a linux command to zip the files -x to exclude the files while zipping, -q to hide the running log, it is unnecessary and waste of memory.
                sh """
                    ls -la
                    zip -q -r catalogue.zip ./* -x ".git" -x "*.zip" 
                    ls -ltr
                """
            }
        }
        stage('Publish Artifact to Nexus Repo') { //Nexus artifact uploader plugin should be installed.
            steps {
                 nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: "${nexusURL}",
                    groupId: 'com.roboshop',
                    version: "${packageVersion}",
                    repository: 'catalogue',
                    credentialsId: 'Nexus_credentials', //Configure the Nexus credentials in jenkins and name it here
                    artifacts: [
                        [artifactId: 'catalogue',
                        classifier: '',
                        file: 'catalogue.zip',
                        type: 'zip']
                    ]
                )
            }
        }
        stage('Deploy') {
            when {
                expression{
                    params.Deploy == 'true' //In parameters the deploy we given as false so if it is true then only this deploy will execute. While doing CI testing it is not necessary to do CD evreytime.
                }
            }
            steps {
                script {
                        def params = [
                            string(name: 'version', value: "$packageVersion"),
                            string(name: 'environment', value: "dev")
                        ]
                        build job: "catalogue-deploy", wait: true, parameters: params //Build job is to pass version & environment to catalogue-downstream job. This stage will wait till downstream job completes.
                    }               //catalogue-deploy is a pipeline name
            }
        }
     }
    // post build
    post { 
        always { 
            echo 'Deleting the workspace folder...!'
            deleteDir() //Once we create the zip file and store it nexus we have to delete the workspace folder.If not we face issue while running second time.
        }
        failure { 
            echo 'This runs when pipeline is failed, used generally to send some alerts'
        }
        success{
            echo 'I will say Hello when pipeline is success'
        }
    }
}
}
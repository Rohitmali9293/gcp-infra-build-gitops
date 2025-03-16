pipeline {
    agent { label 'cm-linux'}
    options {
        timeout (time: 5, unit: 'MINUTES')
        timestamps()
        ansiColor('xtream')
    }
    environment {
        PROJECT_ID = "tf-9293-sandbox"
        DIR = pwd()
    }
    stages {
        stage ('Project info checkout') {
            steps{
                script{
                    withCredentials([string(credentialsId: 'git-token1', variable: 'GIT_TOKEN')]) {
                        echo 'clone the repository...'
                        sh """
                            rm -rf "${PROJECT_ID}"
                            git clone https://${GIT_TOKEN}@github.com/Rohitmali9293/${PROJECT_ID}.git
                            cd "${DIR}/${PROJECT_ID}"
                            git submodule update --init --recursive --remote --force
                            ls -l
                        """
                        def jsonFile = readFile("${DIR}/${PROJECT_ID}/deploy.json")
                        def jsonData = readJSON text: jsonFile
                    // Extract values
                        env.WORKSPACE = jsonData.workspace
                        env.MODULE = jsonData.module
                    // Print to verify
                        echo "Workspace: ${env.WORKSPACE}"
                        echo "Module: ${env.MODULE}"
                    }
                }
            }
        }
    } 
}
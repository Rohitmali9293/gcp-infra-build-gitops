pipeline {
    agent { label 'cm-linux'}
    triggers {
        githubPullRequest() // Auto-triggers on new PR
    }
    options {
        timeout (time: 5, unit: 'MINUTES')
        timestamps()
        ansiColor('xtream')
    }
    environment {
        GIT_REPO = 'https://github.com/Rohitmali9293/tf-9293-sandbox.git'
        JENKINSFILE_REPO = 'https://github.com/Rohitmali9293/gcp-infra-build-gitops.git'
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
        stage('tf-plan') {
            steps {
                withCredentials([file(credentialsId: "$PROJECT_ID", variable: 'keyjason')]) {
                    script {
                        echo 'Running terraform plan...'
                        sh """
                            gcloud auth activate-service-account --key-file="${keyjason}" --project=${PROJECT_ID}
                            cd ${DIR}/${PROJECT_ID}/templates/${MODULE}
                            terraform init -backend-config="bucket=${PROJECT_ID}-terraform-backup" -backend-config="prefix=${WORKSPACE}/${MODULE}"
                            if terraform workspace list | grep -q "\\b$WORKSPACE\\b"; then
                                echo "Workspace '$WORKSPACE' exists. Selecting it..."
                                terraform workspace select "$WORKSPACE"
                            else
                                echo "Workspace '$WORKSPACE' does not exist. Creating and selecting it..."
                                terraform workspace new "$WORKSPACE"
                            fi
                            terraform plan -out=tfplan -var-file=${DIR}/${PROJECT_ID}/variables.tfvars
                        """
                    }
                }
            }
        }
        stage('tf-apply') {
            when {
                branch 'main' // Only deploy if PR is merged to main
            }
            steps {
                withCredentials([file(credentialsId: "$PROJECT_ID", variable: 'keyjason')]) {
                    script {
                        input (message: 'click "procced" to approve', ok: 'procced')
                        echo 'Running terraform apply...'
                        sh """
                            gcloud auth activate-service-account --key-file="${keyjason}" --project=${PROJECT_ID}
                            cd ${DIR}/${PROJECT_ID}/templates/${MODULE}
                            terraform init
                            if terraform workspace list | grep -q "\\b$WORKSPACE\\b"; then
                                echo "Workspace '$WORKSPACE' exists. Selecting it..."
                                terraform workspace select "$WORKSPACE"
                            else
                                echo "Workspace '$WORKSPACE' does not exist. Creating and selecting it..."
                                terraform workspace new "$WORKSPACE"
                            fi
                            terraform apply -auto-approve -var-file=${DIR}/${PROJECT_ID}/variables.tfvars
                        """
                    }
                }
            }
        }
    } 
}
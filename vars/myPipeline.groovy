def call(Map config = [:]) {
    node {
        def mvnHome = tool 'Maven-3.9'

        stage('Checkout') {
            checkout scmGit(branches: [[name: '*/master']],
                            extensions: [],
                            userRemoteConfigs: [[
                                url: 'https://github.com/ramesh9849720900/Setup-CI-CD-with-Github-Jenkins-Maven-and-Tomcat-on-AWS.git'
                            ]])
        }

        stage('Build') {
            sh "${mvnHome}/bin/mvn clean install -f webapp/pom.xml"
        }


        /*
        stage('Unit Test'){
              sh "${mvnHome}/bin/mvn test -f webapp/pom.xml"
              junit 'webapp/target/surefire-reports/*.xml'
        }
        
        stage('Code Coverage'){
        jacoco(execPattern: 'webapp/target/jacoco.exec')
        }


        
        stage('Analysis in SonarQube') {
            withSonarQubeEnv('sonarserver') {
                sh "${mvnHome}/bin/mvn clean install sonar:sonar -U -f webapp/pom.xml"
            }
        }
*/
        
        stage('Docker Build & Push') {
            echo "Starting Docker Build & Push Stage"
            sh 'docker build -t webapp:latest .'
            sh 'docker tag my-image-name:latest ramesh9849720900/webapp:latest'
            withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
                sh 'docker push ramesh9849720900/webapp:latest'
            }
        }


        stage('Kubernetes Deployment') {
            withEnv([
                "KUBECONFIG=${config.kubeconfig ?: '~/.kube/config'}"
            ]) {
                echo "Deploying to Kubernetes cluster on AWS EKS"

                // Copy YAML files from resources
                def svc = libraryResource('webapp/regapp-deploy.yml')
                def svc1 = libraryResource('webapp/regapp-service.yml')
                writeFile file: 'regapp-deploy.yml', text: libraryResource('webapp/regapp-deploy.yml')
                writeFile file: 'regapp-service.yml', text: libraryResource('webapp/regapp-service.yml')

                // Optional: validate config
                sh 'kubectl version --client'
                sh 'kubectl get nodes'

                // Apply deployment and service YAMLs
                sh 'kubectl apply -f regapp-deploy.yml'
                sh 'kubectl apply -f regapp-service.yml'
            }
        }

    
        
        /*
        stage('Push Artifact to GitHub') {
            dir('webapp') {
                withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
                    sh '''
                        git config user.email "rameshrams9849@gmail.com"
                        git config user.name "Ramesh"
                        git fetch origin artifact-upload 
                        git checkout -B artifact-upload origin/artifact-upload
                        cp target/webapp.war .
                        git add webapp.war
                        git commit -m "Add build artifact" || echo "No changes to commit"
                        git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/ramesh9849720900/Setup-CI-CD-with-Github-Jenkins-Maven-and-Tomcat-on-AWS.git HEAD:artifact-upload
                    '''
                }
            }
        }

        stage('Deploy to Tomcat') {
            sh '''
                sudo cp webapp/target/webapp.war /opt/tomcat/webapps/
                sudo systemctl restart tomcat
            '''
        }
        */


    }
}

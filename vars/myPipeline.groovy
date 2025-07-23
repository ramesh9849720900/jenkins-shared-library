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
            sh 'docker tag webapp:latest ramesh9849720900/webapp:latest'
            withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
                sh 'docker push ramesh9849720900/webapp:latest'
            }
        }
       

        stage('Kubernetes Deployment to EKS') {
            withCredentials([usernamePassword(
                credentialsId: 'aws-eks-creds',
                usernameVariable: 'AWS_ACCESS_KEY_ID',
                passwordVariable: 'AWS_SECRET_ACCESS_KEY'
            )]) {
                withEnv([
                    'AWS_DEFAULT_REGION=ap-south-1',  // Replace if needed
                    'CLUSTER_NAME=Java-application'  // Replace with actual EKS cluster name
                ]) {
                    echo "Setting up kubeconfig for EKS"
                    sh '''
                        aws eks update-kubeconfig --name $CLUSTER_NAME --region $AWS_DEFAULT_REGION
                        echo "Running kubectl commands"
                        kubectl get nodes
                        kubectl apply -f resources/webapp/regapp-deploy.yml
                        kubectl apply -f resources/webapp/regapp-service.yml
                    '''
                }
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

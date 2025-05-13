def call() {
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

        stage('Build & Analysis in SonarQube') {
            withSonarQubeEnv('sonarserver') {
                sh "${mvnHome}/bin/mvn clean install sonar:sonar -U -f webapp/pom.xml"
            }
        }

        stage('Push Artifact to GitHub') {
            dir('webapp') {
                sh '''
                    git config user.email "rameshrams9849@gmail.com"
                    git config user.name "Ramesh"
                    git fetch origin artifact-upload 
                    git checkout -B artifact-upload origin/artifact-upload
                    cp target/webapp.war .
                    git add webapp.war
                    git commit -m "Add build artifact" || echo "No changes to commit"
                    git push https://ramesh9849720900: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@github.com/ramesh9849720900/Setup-CI-CD-with-Github-Jenkins-Maven-and-Tomcat-on-AWS.git HEAD:artifact-upload
                '''
            }
        }

        stage('Deploy to Tomcat') {
            sh '''
                sudo cp webapp/target/webapp.war /opt/tomcat/webapps/
                sudo systemctl restart tomcat
            '''
        }
    }
}

#!groovy
env.RELEASE_COMMIT = "1";
env.VERSION_NAME = "";
env.SERVICE_NAME = "csctracker_services_notify"
env.IMAGE_NAME = "csctracker-notify-sync"
env.REPOSITORY_NAME = "NotifySyncServer"

pipeline {
    agent none
    stages {
        stage('CheckBranch') {
            agent any
            steps {
                script {
                    result = sh(script: "git log -1 | grep 'Triggered Build'", returnStatus: true)
                    echo 'result ' + result
                    env.RELEASE_COMMIT = result == 0 ? '0' : '1'
                }
            }
        }
        stage('Build') {
            agent any
            tools {
                maven 'M3'
            }
            when {
                expression { env.RELEASE_COMMIT != '0' }
            }
            steps {
                sh 'mvn clean install'
            }
        }
        stage('Tests') {
            agent any
            tools {
                maven 'M3'
            }
            when {
                expression { env.RELEASE_COMMIT != '0' }
            }
            steps {
                sh 'mvn test'
            }
        }
        stage('Gerar versão') {
            agent any
            tools {
                maven 'M3'
            }
            when {
                expression { env.RELEASE_COMMIT != '0' }
            }
            steps {
                script {
                    echo 'RELEASE_COMMIT ' + env.RELEASE_COMMIT
                    if (env.BRANCH_NAME == 'master') {
                        echo 'Master'
                        VERSION = VersionNumber(versionNumberString: '${BUILD_DATE_FORMATTED, "yy"}.${BUILD_WEEK,XX}.${BUILDS_THIS_WEEK,XXX}')
                        sh 'mvn versions:set versions:commit -DnewVersion=RELEASE'
                        sh 'mvn clean install package -DskipTests'
                    } else {
                        echo 'Dev'
                        VERSION = VersionNumber(versionNumberString: '${BUILD_DATE_FORMATTED, "yyyyMMdd"}.${BUILDS_TODAY}.${BUILD_NUMBER}')
                        VERSION = VERSION + '-SNAPSHOT'
                    }

                    withCredentials([usernamePassword(credentialsId: 'gitHub', passwordVariable: 'password', usernameVariable: 'user')]) {
                        script {

                            echo "Creating a new tag"
                            sh 'git pull https://krlsedu:${password}@github.com/krlsedu/' + env.REPOSITORY_NAME + '.git HEAD:' + env.BRANCH_NAME
                            sh 'mvn versions:set versions:commit -DnewVersion=' + VERSION
                            sh 'mvn clean install package -DskipTests'
                            sh "git add ."
                            sh "git config --global user.email 'krlsedu@gmail.com'"
                            sh "git config --global user.name 'Carlos Eduardo Duarte Schwalm'"
                            sh "git commit -m 'Triggered Build: " + VERSION + "'"
                            sh 'git push https://krlsedu:${password}@github.com/krlsedu/' + env.REPOSITORY_NAME + '.git HEAD:' + env.BRANCH_NAME
                        }
                    }
                    env.VERSION_NAME = VERSION
                }
            }
        }
        stage('Docker image') {
            agent any
            when {
                expression { env.RELEASE_COMMIT != '0' }
            }
            steps {
                sh 'docker build -t krlsedu/' + env.IMAGE_NAME + ':latest -t krlsedu/' + env.IMAGE_NAME + ':' + env.VERSION_NAME + ' .'
            }
        }
        stage('Docker Push') {
            agent any
            when {
                expression { env.RELEASE_COMMIT != '0' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerHub', passwordVariable: 'dockerHubPassword', usernameVariable: 'dockerHubUser')]) {
                    sh "docker login -u ${env.dockerHubUser} -p ${env.dockerHubPassword}"
                    sh 'docker push krlsedu/' + env.IMAGE_NAME
                    sh 'docker push krlsedu/' + env.IMAGE_NAME + ':' + env.VERSION_NAME
                }
            }
        }

        stage('Service update') {
            agent any
            when {
                expression { env.RELEASE_COMMIT != '0' }
            }
            steps {
                script {
                    if (env.BRANCH_NAME == 'master') {
                        withCredentials([string(credentialsId: 'csctracker_token', variable: 'token_csctracker')]) {
                            httpRequest acceptType: 'APPLICATION_JSON',
                                    contentType: 'APPLICATION_JSON',
                                    authorization: 'Bearer ' + env.token_csctracker,
                                    httpMode: 'POST', quiet: true,
                                    requestBody: '''{
                                                       "app" : "Jenkins",
                                                       "text" : "Iniciada a atualização do serviço ''' + env.SERVICE_NAME + ''' para a versão: "''' + env.VERSION_NAME + '''
                                                    }''',
                                    url: 'http://192.168.15.48:8101/notify-sync/message'
                            sh 'docker service update --image krlsedu/' + env.IMAGE_NAME + ':' + env.VERSION_NAME + ' ' + env.SERVICE_NAME
                            httpRequest acceptType: 'APPLICATION_JSON',
                                    contentType: 'APPLICATION_JSON',
                                    authorization: 'Bearer ' + env.token_csctracker,
                                    httpMode: 'POST', quiet: true,
                                    requestBody: '''{
                                                       "app" : "Jenkins",
                                                       "text" : "O serviço ''' + env.SERVICE_NAME + ''' fo atualizado com sucesso para a versão: "''' + env.VERSION_NAME + '''
                                                    }''',
                                    url: 'http://192.168.15.48:8101/notify-sync/message'
                        }
                    } else {
                        withCredentials([usernamePassword(credentialsId: 'developHost', passwordVariable: 'password', usernameVariable: 'user')]) {
                            script {
                                echo "Update remote"
                                def remote = [:]
                                remote.name = 'DevelopHost'
                                remote.host = env.DEVELOP_HOST_IP
                                remote.user = env.user
                                remote.port = 22
                                remote.password = env.password
                                remote.allowAnyHosts = true
                                sshCommand remote: remote, command: "docker service update --image krlsedu/" + env.IMAGE_NAME + ":" + env.VERSION_NAME + " " + env.SERVICE_NAME
                            }
                        }
                    }
                }
            }
        }
    }
}

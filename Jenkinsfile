#!groovy

pipeline {
  agent none
  stages {
    stage('Build') {
      agent any
        tools {
                maven 'M3'
            }
      steps {
        sh 'mvn clean package -DskipTests'
      }
    }
    stage('Tests') {
      agent any
        tools {
                maven 'M3'
            }
      steps {
        sh 'mvn test'
      }
    }
    stage('Docker image') {
      agent any
      steps {
        sh 'docker build -t krlsedu/notify-sync-server:latest .'
      }
    }
    stage('Docker Push') {
      agent any
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerHub', passwordVariable: 'dockerHubPassword', usernameVariable: 'dockerHubUser')]) {
          sh "docker login -u ${env.dockerHubUser} -p ${env.dockerHubPassword}"
          sh 'docker push krlsedu/notify-sync-server'
        }
      }
    }
  }
}
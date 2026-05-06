pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    APP_PORT = '80'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Write Env File') {
      steps {
        withCredentials([
          string(credentialsId: 'qianyu-blog-mysql-root-password', variable: 'MYSQL_ROOT_PASSWORD'),
          string(credentialsId: 'qianyu-blog-db-password', variable: 'DB_PASSWORD'),
          string(credentialsId: 'qianyu-blog-jwt-secret', variable: 'JWT_SECRET'),
          string(credentialsId: 'qianyu-blog-admin-username', variable: 'ADMIN_USERNAME'),
          string(credentialsId: 'qianyu-blog-admin-password', variable: 'ADMIN_PASSWORD')
        ]) {
          sh '''
            cat > .env <<EOF
            APP_PORT=${APP_PORT}
            QIANYU_BLOG_MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
            QIANYU_BLOG_DB_PASSWORD=${DB_PASSWORD}
            QIANYU_BLOG_JWT_SECRET=${JWT_SECRET}
            QIANYU_BLOG_ADMIN_USERNAME=${ADMIN_USERNAME}
            QIANYU_BLOG_ADMIN_PASSWORD=${ADMIN_PASSWORD}
            EOF
          '''
        }
      }
    }

    stage('Deploy') {
      steps {
        sh 'docker compose up -d --build --remove-orphans'
      }
    }

    stage('Health Check') {
      steps {
        sh '''
          for i in $(seq 1 30); do
            if curl -fsS "http://127.0.0.1:${APP_PORT}/api/public/categories" >/dev/null; then
              exit 0
            fi
            sleep 5
          done
          echo "Health check failed"
          docker compose ps
          exit 1
        '''
      }
    }
  }

  post {
    always {
      sh 'docker compose ps || true'
    }
    failure {
      sh 'docker compose logs --tail=200 || true'
    }
  }
}

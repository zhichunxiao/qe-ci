/*
* @GIT_REPO_SSH_URL
* @BRANCH
* @VERSION
*/

// https://cd.pingcap.net/job/release_tikv-java-client/

pipeline {
    agent any

    // 依赖工具
    tools {
        maven "Maven"
    }

    // 环境变量
    environment {
        // Nexus配置
        NEXUS_VERSION = "nexus2"
        NEXUS_PROTOCOL = "https"
        NEXUS_URL = "oss.sonatype.org"
        NEXUS_CREDENTIAL_ID = "ossrh"

        // Git配置
        GIT_CREDENTIAL_ID = "github-sre-bot-ssh"

        // GPG
        GPG_KEY_NAME = "marsishandsome"
    }
    
    // CD Pipeline
    stages {
        stage("GPG") {
            steps {
                script {
                    // GPG Key
                    sh '''
                        curl http://fileserver.pingcap.net/download/gpgkey_pub.gpg -o gpgkey_pub.gpg
                        curl http://fileserver.pingcap.net/download/gpgkey_secret.gpg -o gpgkey_secret.gpg

                        grep -qxF 'use-agent' ~/.gnupg/gpg.conf || echo 'use-agent' > ~/.gnupg/gpg.conf
                        grep -qxF 'pinentry-mode loopback' ~/.gnupg/gpg.conf || echo 'pinentry-mode loopback' > ~/.gnupg/gpg.conf
                        grep -qxF 'batch' ~/.gnupg/gpg.conf || echo 'batch' > ~/.gnupg/gpg.conf
                        grep -qxF 'allow-loopback-pinentry' ~/.gnupg/gpg-agent.conf || echo 'allow-loopback-pinentry' > ~/.gnupg/gpg-agent.conf
                        echo RELOADAGENT | gpg-connect-agent
                        export GPG_TTY=$(tty)

                        gpg --import gpgkey_pub.gpg
                        gpg --import gpgkey_secret.gpg
                    '''
                }
            }
        }
        stage("Clone Code") {
            steps {
                script {
                    // Clone and Checkout Branch
                    git credentialsId: GIT_CREDENTIAL_ID, url: GIT_REPO_SSH_URL
                    sh "git branch -a" // List all branches.
                    sh "git checkout ${BRANCH}" // Checkout to a specific branch in your repo.
                    sh "ls -lart ./*"  // Just to view all the files if needed
                }
            }
        }
        stage("Maven Build & Deploy") {
            steps {
                script {
                    if (VERSION != null && !VERSION.isEmpty()) {
                        sh "mvn versions:set -DnewVersion=${VERSION}"
                    }
                    // sh "mvn clean package -DskipTests=true"
                    sh "mvn clean deploy -DskipTests -Dgpg.skip=false -Djavadoc.skip=false -Dgpg.keyname=${GPG_KEY_NAME}"
                    // sh "mvn clean package -DskipTests -Dgpg.skip=false -Djavadoc.skip=false -Dgpg.keyname=${GPG_KEY_NAME} -Dmaven.wagon.rto=18000000"
                }
            }
        }
        // stage("Publish to Nexus Repository Manager") {
        //     steps {
        //         script {
        //             // 获取jar包产物: target/*.jar
        //             pom = readMavenPom file: "pom.xml";
        //             packageFile = findFiles(glob: "target/*.${pom.packaging}");
        //             echo "${packageFile[0].name} ${packageFile[0].path} ${packageFile[0].directory} ${packageFile[0].length} ${packageFile[0].lastModified}"
                    
        //             // 获取产物仓库
        //             NEXUS_REPOSITORY = "snapshots";
        //             if (!pom.version.contains("-SNAPSHOT")) {
        //                 NEXUS_REPOSITORY = "releases";
        //             }

        //             // 获取产物信息: 文件位置等
        //             artifactPath = packageFile[0].path;
        //             artifactExists = fileExists artifactPath;
        //             version = pom.version;
        //             if (VERSION != null && !VERSION.isEmpty()) {
        //                 version = VERSION;
        //             }
        //             echo "KeyLog: File: ${artifactPath}, group: ${pom.groupId}, packaging: ${pom.packaging}, version ${version}, nexus repo ${NEXUS_REPOSITORY}";

        //             // 上传到中央Nexus仓库
        //             if(artifactExists) {
        //                 nexusArtifactUploader(
        //                     nexusVersion: NEXUS_VERSION,
        //                     protocol: NEXUS_PROTOCOL,
        //                     nexusUrl: NEXUS_URL,
        //                     groupId: pom.groupId,
        //                     version: version,
        //                     repository: NEXUS_REPOSITORY,
        //                     credentialsId: NEXUS_CREDENTIAL_ID,
        //                     artifacts: [
        //                         [artifactId: pom.artifactId,
        //                         classifier: '',
        //                         file: artifactPath,
        //                         type: pom.packaging],
        //                         [artifactId: pom.artifactId,
        //                         classifier: '',
        //                         file: "pom.xml",
        //                         type: "pom"]
        //                     ]
        //                 );
        //             } else {
        //                 error "*** File: ${artifactPath}, could not be found";
        //             }
        //         }
        //     }
        // }
    }
}

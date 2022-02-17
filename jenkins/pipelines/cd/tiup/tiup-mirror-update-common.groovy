node("build_go1130") {
    container("golang") {
        stage("Prepare") {
            println "debug command:\nkubectl -n jenkins-ci exec -ti ${NODE_NAME} bash"
            deleteDir()
        }

        checkout scm
        
        util = load "jenkins/pipelines/cd/tiup/tiup_utils.groovy"

        process(release_tag, sha1, name, params)
    }
}
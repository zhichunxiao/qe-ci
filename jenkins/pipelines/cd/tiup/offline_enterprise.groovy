label = "${JOB_NAME}-${BUILD_NUMBER}"

def run_with_pod(Closure body) {
    def cloud = "kubernetes"
    def namespace = "jenkins-cd"
    def pod_go_docker_image = 'hub.pingcap.net/jenkins/centos7_golang-1.18:add_tool_yajl'
    def jnlp_docker_image = "jenkins/inbound-agent:4.3-4"
    podTemplate(label: label,
            cloud: cloud,
            namespace: namespace,
            idleMinutes: 0,
            containers: [
                    containerTemplate(
                            name: 'golang', alwaysPullImage: false,
                            image: "${pod_go_docker_image}", ttyEnabled: true,
                            resourceRequestCpu: '8000m', resourceRequestMemory: '12Gi',
                            command: '/bin/sh -c', args: 'cat',
                            envVars: [containerEnvVar(key: 'GOPATH', value: '/go')],

                    )
            ],
            volumes: [
                    emptyDirVolume(mountPath: '/tmp', memory: false),
                    emptyDirVolume(mountPath: '/home/jenkins', memory: false)
            ],
    ) {
        node(label) {
            println "debug command:\nkubectl -n ${namespace} exec -ti ${NODE_NAME} bash"
            body()
        }
    }
}

run_with_pod {
    container("golang") {
        stage('generate tags') {
            def cmd = "curl -L -s https://registry.hub.docker.com/v1/repositories/$SOURCE_IMAGE/tags | json_reformat | grep -i name | awk '{print \$2}' | sed 's/\"//g' | sort -u"
            tags = sh(returnStdout: true, script: "$cmd").trim().split("\n")
            println "$tags"
        }

        stage('Build') {
            builds = [:]
            def count = 0
            def tag_list = tags.toList()
            def tag_num = tag_list.size()
            for (tag in tag_list) {
                count += 1
                echo "正在处理第${count}/${tag_num}个版本：${tag}"
                build job: "jenkins-image-syncer", wait: true,
                        parameters: [
                                [$class: 'StringParameterValue', name: 'SOURCE_IMAGE', value: "$SOURCE_IMAGE" + ":" + "$tag"],
                                [$class: 'StringParameterValue', name: 'TARGET_IMAGE', value: "$TARGET_IMAGE" + ":" + "$tag"],
                        ]
            }
        }
    }
}



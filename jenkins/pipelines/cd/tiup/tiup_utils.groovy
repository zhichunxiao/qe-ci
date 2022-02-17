def install_tiup(bin_dir,private_key) {
    sh """
    wget -q https://tiup-mirrors.pingcap.com/tiup-linux-amd64.tar.gz
    sudo tar -zxf tiup-linux-amd64.tar.gz -C ${bin_dir}
    sudo chmod 755 ${bin_dir}/tiup
    rm -rf ~/.tiup
    mkdir -p /home/jenkins/.tiup/bin/
    curl https://tiup-mirrors.pingcap.com/root.json -o /home/jenkins/.tiup/bin/root.json
    mkdir -p ~/.tiup/keys
    set +x
    echo ${private_key} | base64 -d > ~/.tiup/keys/private.json
    set -x
    """
}

def install_tiup_without_key(bin_dir) {
    sh """
    wget -q ${TIUP_MIRROR}/tiup-linux-amd64.tar.gz
    tar -zxf tiup-linux-amd64.tar.gz -C ${bin_dir}
    chmod 755 ${bin_dir}/tiup
    rm -rf ~/.tiup
    mkdir -p ~/.tiup/bin
    curl ${TIUP_MIRROR}/root.json -o ~/.tiup/bin/root.json
    mkdir -p ~/.tiup/keys
    """
}

def download(name, tag, hash, os, arch) {
    if (os == "linux") {
        platform = "centos7"
    } else if (os == "darwin" && arch == "amd64") {
        platform = "darwin"
    } else if (os == "darwin" && arch == "arm64") {
        platform = "darwin-arm64"
    }  else {
        sh """
        exit 1
        """
    }

    tarball_name = "${name}-${os}-${arch}.tar.gz"

    sh """
    wget ${FILE_SERVER_URL}/download/builds/pingcap/${name}/optimization/${tag}/${hash}/${platform}/${tarball_name}
    """

}

def unpack(name, hash, os, arch) {
    tarball_name = "${name}-${os}-${arch}.tar.gz"

    sh """
    tar -zxf ${tarball_name}
    """
}

def process(release_tag, origin_tag, sha1, name, params ) {
    stage("Install tiup") {
        install_tiup "/usr/local/bin", PINGCAP_PRIV_KEY
    }

    if (params.ARCH_X86) {
        stage("tiup release br linux amd64") {
            update name, release_tag, sha1,"linux", "amd64"
        }
    }
    if (params.ARCH_ARM) {
        stage("tiup release br linux arm64") {
            update name, release_tag, sha1, "linux", "arm64"
        }
    }
    if (params.ARCH_MAC) {
        stage("tiup release br darwin amd64") {
            update name, release_tag, sha1, "darwin", "amd64"
        }
    }
    if (params.ARCH_MAC_ARM) {
        stage("tiup release br darwin arm64") {
            update name, release_tag, sha1, "darwin", "arm64"
        }
    }
}

return this

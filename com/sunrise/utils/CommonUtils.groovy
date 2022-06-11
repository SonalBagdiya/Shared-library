package ie.aib;

Map getPipelineOpts(Map opts=[:]) {
    sh 'printenv'
    branch = env.BRANCH_NAME?:''
    if(opts.branch) { branch = opts.branch }

    opts += [
        branch: branch,
        checkoutDisabled: valueFromEnvironment('CHECKOUT_DISABLED'),
        cleanBuildDisabled: valueFromEnvironment('CLEAN_BUILD_DISABLED'),
        sousChefDisabled: valueFromEnvironment('SOUS_CHEF_DISABLED'),
        unitAndIntTestDisabled: valueFromEnvironment('UNIT_AND_INT_TEST_DISABLED'),
        sonarQubeDisabled: valueFromEnvironment('SONAR_DISABLED'),
        docsGenDisabled: valueFromEnvironment('DOCS_DISABLED'),
        deployDisabled: valueFromEnvironment('DEPLOY_DISABLED'),
        acceptanceTestDisabled: valueFromEnvironment('ACCEPTANCE_TEST_DISABLED'),
        archiveDisabled: valueFromEnvironment('ARCHIVE_DISABLED'),
        uploadSnapshotDisabled: valueFromEnvironment('UPLOAD_DISABLED'),
        releaseDisabled: valueFromEnvironment('RELEASE_DISABLED'),
        testCoverageDisabled: valueFromEnvironment('TEST_COVERAGE_DISABLED'),
        jarOnly: valueFromEnvironment('JAR_ONLY'),

        deployEnvironment: valueFromEnvironment('DEPLOY_ENVIRONMENT', 'dev'),

        nightlyJob: branch?.equals('master'),
        trunkJob: env.BRANCH_NAME?.matches('master'),
        pullRequestJob: env.BRANCH_NAME?.matches('PR-\\d+') || env.BRANCH_NAME?.matches('DBBC-\\d+'),
        jks_path: '/etc/ssl/keys/dev',
        jks_pass: 'changeit'
    ]

    opts['releaseableBranch'] = opts.branch?.matches('DBB-\\d+.\\d.\\d-RC') || opts.nightlyJob
    opts['releaseJob'] = opts.releaseableBranch && ! opts.releaseDisabled && (params.PERFORM_RELEASE == true)
    opts['cleanWorkspaceDisabled'] = valueFromEnvironment('CLEAN_WORKSPACE_DISABLED',!(opts.nightlyJob || opts.releaseJob))

    opts['upstream'] = currentBuild.rawBuild.getCause(hudson.model.Cause$UpstreamCause)
    opts['pullRequestDeployHost'] = 'rhibbvd1.mid.aib.pri' //'ci-pr'
    opts['nightlyDeployHost'] = 'rhibbvd1.mid.aib.pri' //'ci-nightly'
    opts['deployHost'] = (opts.nightlyJob || opts.releaseableBranch) ? opts.nightlyDeployHost : opts.pullRequestDeployHost


    println "isNightly: ${opts.nightlyJob}, isRelease: ${opts.releaseJob}, isPullRequest: ${opts.pullRequestJob}, isTrunk: ${opts.trunkJob}"
    try {
        if (opts.pullRequestJob) {
            currentBuild.displayName = "#${env.BUILD_NUMBER} ${(env.CHANGE_TITLE)?:env.CHANGE_BRANCH} PR #${env.CHANGE_ID}"
        } else {
            currentBuild.displayName = "#${env.BUILD_NUMBER} ${branch.startsWith('origin')?branch.substring(7):branch}"
        }
    } catch (error) {
        println "PR name setting error $error"
    }

    println opts
    return opts
}

boolean valueFromEnvironment(String envVariableName, defaultValue = false){
    value = env[envVariableName] ?: defaultValue
    if(value == 'true' || value == 'false') {
        value = value.toBoolean()
    }
    return value
}

def downloadKeys(String jks_path, String jks_pass) {
    trust_nexus_path = 'https://nexus.aib.pri/nexus/service/local/artifact/maven/content?r=releases&g=ie.aib.dbbe.security'
    sh """
        wget -q '${trust_nexus_path}&p=cacerts&v=LATEST&a=trust&c=cert'      -O $build_java/lib/security/cacerts
        wget -q '${trust_nexus_path}&p=jks&v=LATEST&a=trust&c=cert'      -O /root/cacerts_withAIB.jks
    """
}

Map loadServiceInfo() {
    serviceProperties = [:]
    jarOnly = valueFromEnvironment('JAR_ONLY')
    if (fileExists('service.info')) {

        def props = readFile encoding: 'UTF-8', file: 'service.info'
        def propKeyValues = props.tokenize()
        for (String propKeyValue : propKeyValues) {
            def keyValue = propKeyValue.split('=')
            if (keyValue.length > 1) {
                serviceProperties[keyValue[0]] = (keyValue[1]) ?: ''
            } else {
                serviceProperties[keyValue[0]] = ''
            }
        }
    } else if (jarOnly){
        sh 'ls build/libs > version.info'
        serviceProperties['version']=readFile ('version.info').replaceAll('\\.jar$','')
    }
    return serviceProperties
}

def dbbeReleaseBranch(String branch){
    if (branch == null){
        branch = env.BRANCH_NAME
    } else return branch
}


String getEnvName() {
    envName = env.JOB_NAME.contains('T3A')?'dbbt3':
              env.JOB_NAME.contains('T3B')?'dbbt3b':
              env.JOB_NAME.contains('T3C')?'dbbt3c':
              env.JOB_NAME.contains('T1')?'dbbt1':
              env.JOB_NAME.contains('T2B')?'dbbt2b':
              env.JOB_NAME.contains('T2')?'dbbt2':
              env.JOB_NAME.contains('T4')?'dbbt4':
              env.JOB_NAME.contains('T5')?'dbbt5':
              env.JOB_NAME.contains('T3N')?'dbbt3nightly':
              env.JOB_NAME.contains('DrStrange')?'dbbt3d':
              env.JOB_NAME.contains('T3E')?'dbbt3e':''
    return envName
}

Map getAnsibleEnvNameOrNodeHost() {
    return [
            "dbbt1": [
                    'node1': 'rhdbbt1vt1.mid.aib.pri',
                    'node2': 'rhdbbt1vt2.mid.aib.pri',
                    'ansibleEnvName': 't1_staging'
            ],
            "dbbt2b": [
                    'node1': 'rhdbbt2bvt1.mid.aib.pri',
                    'node2': 'rhdbbt2bvt2.mid.aib.pri',
                    'ansibleEnvName': 't2_beta'
            ],
            "dbbt2": [
                    'node1': 'rhdbbt2vt1.mid.aib.pri',
                    'node2': 'rhdbbt2vt2.mid.aib.pri',
                    'ansibleEnvName': 't2_stable'
            ],
            "dbbt4": [
                    'node1': 'rhdbbt4vt1.mid.aib.pri',
                    'node2': 'rhdbbt4vt2.mid.aib.pri',
                    'ansibleEnvName': 't4_external'
            ],
            "dbbt5": [
                    'node1': 'rhdbbt5vt1.mid.aib.pri',
                    'node2': 'rhdbbt5vt2.mid.aib.pri',
                    'ansibleEnvName': 't5_migration'
            ],
            "dbbt3b": [
                    'node1': 'rhdbbintvd1.mid.aib.pri',
                    'node2': 'rhdbbintvd2.mid.aib.pri',
                    'ansibleEnvName': 't3_dev'
            ],
            "dbbt3c": [
                    'node1': 'rhdbbt3cvt1.mid.aib.pri',
                    'node2': 'rhdbbt3cvt2.mid.aib.pri',
                    'ansibleEnvName': 't3c_orange'
            ],
            "dbbt3d": [
                    'node1': 'rhdbbt3dvt1.mid.aib.pri',
                    'node2': 'rhdbbt3dvt2.mid.aib.pri',
                    'ansibleEnvName': 't3d_rstrange'
            ],
            "dbbt3": [
                    'node1': 'rhdbbt3vt1.mid.aib.pri',
                    'node2': 'rhdbbt3vt2.mid.aib.pri',
                    'ansibleEnvName': 't3_latest'
            ],
            "dbbt3nightly": [
                    'node1': 'rhdbbtnightlyvd1.mid.aib.pri',
                    'node2': 'rhibbvd2.mid.aib.pri',
                    'ansibleEnvName': 't3_nightly'
            ],
            "dbbt3e": [
                    'node1': 'rhdbbt3evt1.mid.aib.pri',
                    'node2': 'rhdbbt3evt2.mid.aib.pri',
                    'ansibleEnvName': 't3e_relteam'
            ]
    ]
}


def prepareMavenSettingsXml(){
    // settings.xml file download from devops stash repo
    url = 'https://gitstash.aib.pri/projects/DBBE/repos/devops/raw/automation/settings_min_proxy_bb.xml?at=refs%2Fheads%2Fmaster'
    sh "curl -u \${GIT_CREDENTIALS_USR}:\${GIT_CREDENTIALS_PSW} '${url}' -o settings.xml"
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: env.NEXUS_CREDENTIALS_ID,passwordVariable: 'NEXUS_PWD', usernameVariable: 'NEXUS_USER']]) {
        sh """
           sed -i -e '/<username>aib_portal/ s#<user.*#<username>${env.NEXUS_USER}</username>#g' ${WORKSPACE}/settings.xml
           sed -i -e '/<password>PASSWORD_PLACEHOLDER/ s#<pass.*#<password>${env.NEXUS_PWD}</password>#g' ${WORKSPACE}/settings.xml
           sed -i -e '/<password>GIT_PASSWORD_PLACEHOLDER/ s#<pass.*#<password>${GIT_CREDENTIALS_PSW}</password>#g' ${WORKSPACE}/settings.xml
        """
    }
}
def prepareVersionNumber(String branch,String pom,String versionVar,String nexusGroup,String nexusArtifact,boolean release = false){
    pomRegexp="cat $pom | grep /$versionVar | head -1"
    sh "$pomRegexp > version.info"
    def version = readFile('version.info').trim().replaceAll('</.*$','').replaceAll('^.*>','')
    String releaseBranchPattern = 'DBB-\\d+.\\d.\\d-RC'
    String thisBranch = branch
    String parentBranch = ''
    if(thisBranch.matches(releaseBranchPattern)) {
        parentBranch = thisBranch
    }else {
        parentBranch = sh(script: 'git branch -a --contains `git rev-parse --verify HEAD` | grep origin | sed \'s/.*origin\\///\'', returnStdout: true).split('\n')[0].trim()
    }

    if (parentBranch.contains("/")){
        parentBranch = parentBranch.split("/")[0]
    }

    println "Found branch $parentBranch that is release ${parentBranch.matches(releaseBranchPattern)}"

    dbbeRelease = findRelease(parentBranch, releaseBranchPattern)

    version = version.replaceAll('-SNAPSHOT','')
    def versions = ''
    if(versionVar == "version"){
        versions = version.tokenize('.')
        version = "${versions[0]}.${versions[1]}"
    }
    int sequence = 0
    sh "wget --no-check https://nexus.aib.pri/nexus/content/repositories/releases/$nexusGroup/$nexusArtifact/maven-metadata.xml &2> /dev/null "
    if (fileExists('maven-metadata.xml')){
        println "Got maven metadata"
        List<String> mavenData = readFile('maven-metadata.xml').split('\n') as List
        println mavenData
        for (String line : mavenData){
            if (line.contains('<version>') && line.contains("$version.$dbbeRelease.")){
                int currentSequence = line.split('\\.').last().replaceAll('</.*','') as int
                sequence = (sequence > currentSequence)?sequence : currentSequence +1
            }
        }
        println "Next sequense version is $sequence"
    }
    if (!release){
        sequence = (sequence == 0)?sequence:sequence -1
    }
    println "Building $nexusArtifact in version ${version}.${dbbeRelease}.${sequence}"
    return "${version}.${dbbeRelease}.${sequence}"
}
def releasableBranch(String branch){
    releaseableBranch = branch?.matches('DBB-\\d+.\\d.\\d-RC')
    return releaseableBranch
}

def releaseArtifact(String branch){
    return (releasableBranch(branch) && (PERFORM_RELEASE == 'true'))
}

def findRelease(String branch, String releaseBranchPattern){
    if (branch == 'master' || branch == 'develop') {
        release = '0'
    } else if(branch.matches(releaseBranchPattern)) {
        release = branch.split('-')[1].split('\\.')[0]
    } else {
        println "Cannot find DBBE release from $branch"
        release = branch
    }
    release
}

def String getReleaseNumber() {
    return getReleaseNumber(env.JOB_NAME)
}

def String getReleaseNumber(String branch) {
    def release = branch =~ /R[0-9][0-9](([_][0-9])|([.][0-9]))?/
    return release[0][0]
}

def String getReleaseBranch(String release) {
    String branch = ''
    if(release.contains('_') || release.contains('.')){
        branch = "DBB-${release.replace('R', '').replace('_','.')}.0-RC"
    } else {
        branch = "DBB-${release.replace('R', '')}.0.0-RC"
    }
    return branch
}

return this

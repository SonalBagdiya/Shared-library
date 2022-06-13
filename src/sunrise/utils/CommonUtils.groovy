package src.sunrise;

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

return this

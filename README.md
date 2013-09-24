cob-pipeline-plugin
===================

This plugin provides the possibility to configure a user-specific build and test pipeline for ROS repositories inside Jenkins.

For further informations check the [README](https://github.com/ipa320/jenkins_setup/blob/master/README.md) of the [jenkins\_setup](https://github.com/ipa320/jenkins_setup) repository.

## Short Developers Guide

All necessary information how to develop a Jenkins plugin can be found in the [Extend Jenkins](https://wiki.jenkins-ci.org/display/JENKINS/Extend+Jenkins) section in the [Jenkins Wiki](https://wiki.jenkins-ci.org).
The [Plugin Tutorial](https://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial) gives helpful overview of the usage of the development tool Maven and the most useful commands.

### Enhance this plugin

* [Set up a development environment](https://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial#Plugintutorial-SettingUpEnvironment)
* Clone this repository into your workspace:

    ```bash
    git clone git@github.com:ipa320/cob-pipeline-plugin.git
    ```

* After you enhanced something you can easily test it on a local Jenkins
  instance:

    ```bash
    export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n"
    mvn clean && mvn hpi:run
    ```

    The Jenkins server is avialable on [http://localhost:8080/jenkins](http://localhost:8080/jenkins).
    Changes in files (e.g. `config.jelly`, `help-message.html`, ..) below the `src/main/resources/..' folder can be seen online.
    You don't need to restart the whole Jenkins instance, just reload the site.

### Distribute plugin
When you reach a state you want to want to distribute, you can create a image with:

```bash
mvn clean && mvn package
```

This should create the `target/cob-pipeline.hpi` file, which can be installed in Jenkins by store it in `/var/lib/jenkins/plugins/`.
Reload Jenkins to activate it.

More informations are available on the official [Plugin Tutorial Wiki Site](https://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial).

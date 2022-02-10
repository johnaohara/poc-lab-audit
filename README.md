# poc-lab-audit

## Import RH root CA certificate for gitlab signed certifcate
keytool -import -alias rh_root_ca -cacerts -file RedHatITRootCA.crt


# Running audit

```
java -jar lab-audit-*-with-dependencies.jar -u {GITLAB_DEPLOY_TOKEN_USERNAME} -p {GITLAB_DEPLOY_TOKEN_PASSWORD} -r https://gitlab.cee.redhat.com/middlewareperformance/performance-lab-scripts.git -d /tmp/performance-lab-scripts
```
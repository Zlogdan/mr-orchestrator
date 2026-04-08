# mr-orchestrator

## SSL / PKIX ошибка при доступе к GitLab

Если при загрузке веток появляется ошибка вида:

- `PKIX path building failed`
- `unable to find valid certification path to requested target`

значит сертификат GitLab не доверен JVM.

Варианты решения:

1. Добавить сертификат GitLab в системный `cacerts` JRE/JDK.
2. Либо создать отдельный truststore и указать его в [config.yaml](config.yaml):

- `gitlab.trustStorePath`
- `gitlab.trustStorePassword`
- `gitlab.trustStoreType` (`JKS` или `PKCS12`)

3. Для временной локальной разработки можно отключить SSL-проверку:

- `gitlab.disableSslVerification: true`

⚠️ Не используйте `disableSslVerification=true` в production.

Пример находится в [config.yaml](config.yaml).
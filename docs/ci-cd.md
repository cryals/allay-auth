# CI/CD и релизы

В проекте есть три workflow и Dependabot-настройка.

## `ci.yml`

Запускается на push, pull request и вручную через `workflow_dispatch`.

Что делает:

1. запускает lint job;
2. проверяет YAML через `yamllint`;
3. проверяет Markdown через `markdownlint-cli2`;
4. проверяет GitHub Actions через `actionlint`;
5. собирает Maven project на Java 21 и Java 25;
6. запускает `mvn -B -DskipTests validate`;
7. запускает `mvn -B -DskipTests package`;
8. загружает Java 21 jar как artifact;
9. собирает MkDocs документацию в strict mode.

Почему Java 21 и 25: серверная цель - Java 21, но локально у проекта может быть Java 25. Matrix гарантирует, что artifact остается Java 21-compatible за счет Maven compiler `release=21`, а сборка не ломается на более новом JDK.

## `docs.yml`

Запускается на push в `main`, pull request по docs-файлам и вручную через `workflow_dispatch`.

Что делает:

1. собирает MkDocs site;
2. на pull request останавливается после build-проверки;
3. на push/main деплоит HTML-сайт в GitHub Pages через official Pages actions;
4. на push/main синхронизирует Markdown-документацию в GitHub Wiki repository.

Для GitHub Pages в настройках repository нужно выбрать source `GitHub Actions`.

Для wiki нужно, чтобы в настройках repository была включена Wiki. Workflow пушит в:

```text
https://github.com/cryals/allay-auth.wiki.git
```

## `release.yml`

Запускается при push тега:

```text
v*
```

Например:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Workflow:

1. собирает jar;
2. переименовывает artifact в `AllayAuth-<tag>.jar`;
3. проверяет, что tag похож на semver: `v1.2.3`;
4. при ручном запуске checkout-ит указанный tag;
5. создает SHA-256 checksum;
6. создает GitHub Release через `gh release create`;
7. прикрепляет jar и `.sha256` к release;
8. генерирует release notes автоматически.

## Почему release по тегам

Релиз должен быть явным событием. Push в `main` может быть обычной разработкой, а tag `v1.0.0` означает стабильную точку, которую можно отдавать владельцам серверов.

## Почему docs deploy отдельно

Документация меняется чаще, чем jar release. Отдельный workflow ускоряет публикацию wiki/pages без создания релиза.

## Dependabot

`.github/dependabot.yml` раз в неделю открывает PR для:

- Maven dependencies;
- GitHub Actions versions.

Это помогает обновлять JDA, Paper API, JDBC drivers и official actions без ручного мониторинга.

## Локальная проверка перед push

Минимальный набор:

```bash
mvn -B -DskipTests package
python -m mkdocs build --strict
yamllint .
npx --yes markdownlint-cli2
```

`actionlint` в CI запускается через Docker image `rhysd/actionlint:1.7.7`.

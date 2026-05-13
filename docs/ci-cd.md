# CI/CD и релизы

В проекте есть три workflow.

## `ci.yml`

Запускается на push и pull request.

Что делает:

1. checkout repository;
2. ставит Java 21 через `actions/setup-java`;
3. кеширует Maven dependencies;
4. запускает `mvn -B -DskipTests package`;
5. загружает jar как artifact.

## `docs.yml`

Запускается на push в `main`, если менялись docs, `mkdocs.yml` или workflow, а также вручную через `workflow_dispatch`.

Что делает:

1. собирает MkDocs site;
2. деплоит HTML-сайт в GitHub Pages через official Pages actions;
3. синхронизирует Markdown-документацию в GitHub Wiki repository.

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
3. создает GitHub Release через `gh release create`;
4. прикрепляет jar к release;
5. генерирует release notes автоматически.

## Почему release по тегам

Релиз должен быть явным событием. Push в `main` может быть обычной разработкой, а tag `v1.0.0` означает стабильную точку, которую можно отдавать владельцам серверов.

## Почему docs deploy отдельно

Документация меняется чаще, чем jar release. Отдельный workflow ускоряет публикацию wiki/pages без создания релиза.

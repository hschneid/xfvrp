# Dependabot Auto-Merge

Der Workflow `dependabot-automerge.yml` aktiviert Squash-Auto-Merge nur für
Dependabot-PRs mit Patch- oder Minor-Updates nach `master`. Major-Updates bleiben
manuell. Ein Minor-Update ist nicht automatisch fehlerfrei: Die Sicherheit hängt
von der Testabdeckung und den verpflichtenden GitHub-Prüfungen ab.

## Vor der Aktivierung auf GitHub

1. In **Settings → General → Pull Requests** sowohl **Allow auto-merge** als auch
   **Allow squash merging** aktivieren.
2. Für `master` einen Branch-Schutz oder ein Ruleset einrichten, das Pull Requests
   und erfolgreiche Statusprüfungen vor dem Merge verlangt. Mindestens den
   Gradle-Build (`build`) und die Tests (`build-and-test`) verpflichtend machen.
   Die genauen Check-Namen nach dem ersten Workflow-Lauf in GitHub auswählen.
   Für eine verpflichtende Sicherheitsanalyse zusätzlich CodeQL (`analyze`)
   aufnehmen. Ohne verpflichtende Checks kann `gh pr merge --auto` sofort mergen.
3. Unter **Settings → Actions → General → Workflow permissions** erlauben, dass
   GitHub Actions Pull Requests freigeben darf.
4. Die Workflow-Änderungen per Pull Request prüfen und nach `master` übernehmen.
   Bereits offene Dependabot-PRs benötigen anschließend ein neues PR-Ereignis,
   beispielsweise durch einen Dependabot-Rebase.

Die lokale Vorbereitung ändert keine dieser GitHub-Einstellungen.

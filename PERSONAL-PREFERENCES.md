# Personal Workflow Preferences
Personal conventions for working with Claude Code effectively.

## Long response convention
When a response (plan, diff, analysis, explanation) is longer than 10 lines, ALWAYS:
1. Print the response to the terminal as normal
2. Also write it to `/tmp/claude_response.txt` (overwrite each time — latest response only)

This makes long responses easy to review in an editor rather than scrolling the terminal.
The `/tmp/` folder is already in `.gitignore`.

Example:
```bash
cat > /tmp/claude_response.txt << 'EOF'
Your response here
EOF
```

## Branching strategy

### Branches
- **devel**: active development. May be unstable.
- **staging**: known-good, tested state. Merge from devel when quality is validated and tests
  pass. Used for user testing and demos.
- **master**: public-facing, portfolio-ready. Merge from staging after tester validation.

### When to merge devel → staging
- All tests pass
- Manual quality test confirms the app behaves as expected
- No TEMPORARY code committed
- You are happy showing this to someone else

### End of session checklist
At the end of every development session, Claude should ask:

> "Before we wrap up — do you want to merge devel into staging? Checklist:
> ✅ All tests passing?
> ✅ App quality feels good?
> ✅ No TEMPORARY code committed?
> ✅ Happy to show this to someone else?
> If yes to all → merge now while the session context is fresh."

The developer should make it a habit to signal the end of a session explicitly so this
checklist is not skipped.

### Convention
At the end of each session where quality feels good:
```powershell
git checkout staging
git merge devel
git push origin staging
```

Do not merge experimental features (e.g. new architecture) to staging until they are
validated and ready to replace the previous approach.

### Version management
A VERSION file at the repo root tracks the current version (semver: major.minor.patch).

Invariant: devel must never be behind staging or master on version number.

After every commit and push to devel (routine dev work, not the merge pipeline):
  Increment patch version (0.8.1 → 0.8.2)
  Update VERSION file
  Update app.version in src/main/resources/application.properties
  Display: "Version bumped to X.X.X"

Before running the devel → staging → master merge pipeline:
  1. On devel FIRST — increment minor version, reset patch to 0 (0.8.2 → 0.9.0)
  2. Update VERSION file
  3. Update app.version in src/main/resources/application.properties
  4. Commit and push this bump to devel
  5. THEN run the merge pipeline: devel → staging → master. The fast-forward
     merges carry the already-bumped version through to staging and master —
     no separate bump on staging or master.
  Display: "Version bumped to X.X.X"

(Bumping on devel before merging — rather than on staging after merging — is
what keeps devel from ever falling behind staging/master.)

## Post-merge verification (ALWAYS do after any merge)

After any merge (devel → staging or staging → master):

1. Confirm the merged branch is up to date:
   ```powershell
   git log staging..devel --oneline
   ```
   (should return empty — means staging has everything devel has)

2. Switch back to devel immediately:
   ```powershell
   git checkout devel
   ```

3. Confirm current branch is devel:
   ```powershell
   git branch --show-current
   ```
   (must show "devel" before continuing any work)

This prevents accidentally developing on staging or master after a merge.
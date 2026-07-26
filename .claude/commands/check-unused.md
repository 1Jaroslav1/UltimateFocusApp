Scan `app/src/main/java/` for dead code. Look for:

- Unused imports
- Unused private functions or variables
- Unreachable code branches
- Composable functions that are defined but never called
- Parameters that are passed but never used

List each finding with file path and line number. Only report things you are confident are unused.

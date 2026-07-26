Review the codebase for duplicated or copy-pasted code in `app/src/main/java/`. Look for:

- Repeated logic across files that could be extracted into shared utility functions
- Similar UI components or Composable functions that could be unified
- Duplicated string constants, color values, or configuration values
- Copy-pasted blocks with only minor differences (parameters, names)

For each finding:
1. Show the duplicated code locations (file paths and line numbers)
2. Explain why it's a problem
3. Suggest a specific refactoring with a code example

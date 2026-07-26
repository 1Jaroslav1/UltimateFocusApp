Review the current uncommitted changes (`git diff`) and look for opportunities to simplify:

- Overly complex conditional logic that could be flattened
- Verbose code that could use Kotlin idioms (scope functions, extension functions, destructuring)
- Unnecessary abstractions or wrapper layers
- Code that could leverage existing Android/Compose/Kotlin APIs instead of reimplementing

Suggest concrete simplifications with before/after code examples. Only suggest changes that clearly improve readability without changing behavior.

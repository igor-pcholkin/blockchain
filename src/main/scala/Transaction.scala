sealed trait ScriptEntry

class Command extends ScriptEntry
class Data extends ScriptEntry

case class Script(entries: List[ScriptEntry])

case class Transaction(input: Transaction, output: Script)


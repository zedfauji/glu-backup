class MonitorScript
{
 /* static String CMD = 
    "uptime | grep -o '[0-9]\\+\\.[0-9]\\+*' | xargs"*/

  def monitor = {
try
{
    // capturing current state
/*    def currentError = stateManager.state.error
    def newError = null
  */
def currentState = stateManager.state.currentState
def currentError = stateManager.state.error
log.warn "${currentState}"
log.warn "${currentError}"
def newState = null
def newError = null

def run_ins= shell.exec("sh /cacheDir/glu_scripts/service_stale_instance_monitor.sh")
log.info "Got no of Stale instance running -> ${run_ins}"

	if(run_ins != '0')
		{
			newState = 'stopped'
			newError = 'Found Stale instances Runinng #TimeToCheck'
			log.warn "${newError} => forcing new state ${newState}"
		}
	else
		{
			newState = 'running'
                        log.info " Stale Running instances not found #Hazzah"
		}
	if(newState)
		{
		stateManager.forceChangeState(newState, newError)
		log.debug "Server Monitor: ${stateManager.state.currentState}"
                //log.info " No No No Aaahhaann , Stale Instance are running  "
		}
}
catch(Throwable th)
    {
      log.warn "Exception while running Stale instance Monitor: ${th.message}"
      log.debug("Exception while running Stale Instance Monitor (ignored)", th)

        }



 }
 
 def start = { timers.schedule(timer: monitor, repeatFrequency: "15s") }
 def stop = { timers.cancel(timer: monitor) }
}

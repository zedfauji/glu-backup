
import org.linkedin.glu.agent.api.ShellExecException
import org.linkedin.glu.agent.api.ScriptExecutionException
import org.linkedin.glu.agent.api.ScriptExecutionCauseException

class StartStop
{
def appState = "..."
def servState = "..."
public void safeuninstall()
{
ScriptExecutionCauseException ex = scriptShouldFail {
      uninstall()
    }
}



/*public void safeinstall()
{
ScriptExecutionCauseException xz = scriptShouldFail {
        install()
        }
}*/
public void safestart()
{
ScriptExecutionCauseException xz = scriptShouldFail {
        start()
        }
}


  def install = {
    log.info "Installing..."
        log.info "Install Phase started"
try{
    shell.exec("sh -xv /cacheDir/glu_scripts/service_install.sh ${params.service} ${params.version} ${params.port} >> /dev/null  2>&1")
}
catch (ShellExecException u)
    {
      return null
    }

        servState = "Installed"

//        timers.schedule(timer: "monitor", repeatFrequency: "1s")
        log.info "Service successfully installed and monitoring initiated"
  }

  def configure = {
    log.info "Nothing to Configure as such."
    shell.exec("sleep 5")
        servState = "Configured"
  }
def pid 
  def start = {
    log.info "Starting service"
        log.info "running script service_start for ${params.service} version ${params.version} on ${params.port}"
        shell.exec("sh /cacheDir/glu_scripts/service_start.sh ${params.service} ${params.version} ${params.port}")
        shell.waitFor(timeout:'5s' , heartbeat: '500') { duration ->
        
		 pid = isServiceUp()
		}
try 
{
	
	
		shell.waitFor(timeout: '30s', heartbeat: '60s') { duration ->
		log.info "${duration}: Waiting for service to be up"
		if(pid)
		log.info "Service has came up "
		else
		shell.fail("Service could not start , Please check log files")
		}
}

catch(Throwable  net)
{
//	return null
   log.info "error running start delay"

} 
        servState = "Started"
        timers.schedule(timer: "monitor", repeatFrequency: "60s")
        Thread.sleep(10000)
}

private Integer isServiceUp()
  {
    try
    {
		log.info "started isServiceUp"
		def output= shell.exec("sh /cacheDir/glu_scripts/service_monitor.sh ${params.service} ${params.port}")
		log.info "${output}"
		 if( output == 'null')
                {
                        return null
                }
			else
				return output as int
        }
	catch(ShellExecException fe)
    {
      return null
    }
	}
  def stop = {
    log.info "Stopping Service"
        shell.exec("sh /cacheDir/glu_scripts/service_stop.sh ${params.service} ${params.port}")
        log.info "current state is ${state}"
        servState = "Stopped"
        timers.cancel(timer: "monitor")
  }

  def unconfigure = {
    log.info "Nothing to UnConfigure as such."
        servState = "Unconfigured"
         timers.cancel(timer: "monitor")
        log.info "cancelled timer of ${mountPoint}"
  }

  def uninstall = {
try
{
    log.info "Uninstalling..."
//      timers.cancel(timer: "monitor")
        shell.exec("sh /cacheDir/glu_scripts/service_uninstall.sh ${params.service} ${params.version} ${params.version}")

        servState = "Uninstalled"
  }
catch(Throwable un)
    {
        log.info("Error Running Uninstall phase")
        log.debug("Error Running Uninstall phase")
        }
}




def monitor = {

try
{
def currentState = stateManager.state.currentState
def currentError = stateManager.state.error
log.warn "${currentState}"
log.warn "${currentError}"
def newState = null
def newError = null

shell.waitFor(timeout: '30s', heartbeat: '60s'){ duration ->
def output= shell.exec("sh /cacheDir/glu_scripts/service_monitor.sh ${params.service} ${params.port}")
log.info "${output}"
//log.info "_____________________| -=ZeD a.k.a Fauji |________________"
//log.info "---- More quiter you , the more you be able hear ----"
log.info "Monitoring Service ${params.service} version ${params.version} on port ${params.port}"
         if(output == 'null')
                {
                        newState = 'stopped'
                        newError = 'Service is Stopped'
                        log.warn "${newError} => forcing new state ${newState}"
                }
                else
                {
                        newState = 'running'
//                      log.info " Ahha Service Is running All Hail To Gr8 GLU matched running "
                        //log.info "_____________ Monitoring Ended ___________ -=Fauji=-"

                        log.info "${params.service} Service is running with PID = ${output}"
                }
        if(newState)
        {
                stateManager.forceChangeState(newState, newError)
              log.debug "Server Monitor: ${stateManager.state.currentState}"
                log.info " Service Not Running "
        //      log.info "_____________ Monitoring Ended ___________ -=Fauji=-"

        }

                log.info "#Monitor Ended #Sad #Sorrow #Respect Soon will be reborn"
                                        }
}
catch(Throwable th)
    {
      log.warn "Exception while running serverMonitor: ${th.message}"
      log.debug("Exception while running serverMonitor (ignored)", th)

        }
}


}


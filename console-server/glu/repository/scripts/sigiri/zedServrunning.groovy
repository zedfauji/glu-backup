class ServMonitorcript
{

def monitor = {


try
{
shell.waitFor(timeout: '30s',hearbeat: '60s') { duration ->
log.info "Sending data to graphite1 and graphite2"
shell.exec("sh /cacheDir/glu_scripts/service_running_monitor.sh")
log.info "Data sent"

}
}
catch(Throwable th)
    {
      log.warn "Exception while running serv_Monitor: ${th.message}"
      log.debug("Exception while running serv_Monitor (ignored)",th)

        }
}
def start = { timers.schedule(timer: monitor, repeatFrequency: "5s") }
 def stop = { timers.cancel(timer: monitor) }
}

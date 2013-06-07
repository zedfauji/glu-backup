import org.linkedin.glu.agent.api.ShellExecException
import org.linkedin.glu.agent.api.ScriptExecutionException
import org.linkedin.glu.agent.api.ScriptExecutionCauseException

class loadbalancing {

def  String NodeName
def String ServiceName

def start = {
	log.info("starting load balancing")
         timers.schedule(timer: loadbalance, repeatFrequency: "15s")
}

        def stop = {
                timers.cancel(timer: loadbalance)
        }

def NodeNameList = ["sv-sigiri1","sv-sigiri2","sv-sigiri3","sv-sigiri4","sv-sigiri5","sv-sigiri6"]

def loadbalance = {
        //This the main function.
	log.info ("starting load balance method")
	log.warn ("warning :starting load balance method")
  //      gen_nodeName() //1. we generate servers list
		gennode1()


}


def gennode1 =
{
log.info ("started gennode1")

def a = 1;
NodeNameList.each
{ 
		log.info("$NodeNameList")
		log.info("it = ${it}")
		NodeName = it as String 
//		NodeName = $it 
		log.info("${NodeName}")

			 if(!isServerAvail())
	                {
		                        //Now server is not availaible. Starting load balancing mechanism
                	        log.info("Server has gone down, Now starting fault tolerance")

	                        doRetry()

        	        }
}
}


/*def  Integer ServRunning()
{ 

	log.info("Started ServRunning passing ${NodeName} ${params.servicename}")
        shell.waitFor(timeout: '30s', heartbeat: '60s'){ duration ->
                        def output= shell.exec(" sh /cacheDir/glu_scripts/load_service_monitor.sh ${NodeName} ${params.service}")
                        log.info "${output}"

                                if(output == 'null')
                                        {
                                                log.warn ("No ${params.service} is running on this server.")
                                                return null
                                        }
				else
					return output as int


}
}*/

def  boolean isServiceDown()  
{ 
        ServRunning() == null
}

public void safestart()
{
ScriptExecutionCauseException ex = scriptShouldFail {
      start()
    }
}
public void safeloadbalance()
{
	ScriptExecutionCauseException vx = scriptShouldFail {
		loadbalance()
		}
} 
def  Integer isServerup()  {
	log.info ("started isServerup()")

        //check if server is in stale list for this service.
        def DownServList = []
	def resp_ssh = 
		log.info("NodeName = ${NodeName}")
//		try
//			{		
		                 resp_ssh = shell.exec("sh /cacheDir/glu_scripts/glucheck.sh  ${NodeName} ")
//			}
//				catch (ShellExecException u)
//					{	return null
//					}
		
		log.info ("${resp_ssh}")
                if (  resp_ssh < 0)
                {
                        log.info ("${NodeName} server is up and running")
			return resp_ssh
		}
                else
                {
		log.info("Adding server to down server list")
                DownServList << NodeName // add server to a down server list
                log.info ("Server ${NodeName} has been flagged as Down Server ")
                log.info ("Shifting running services to another node.")
		return null

                }



        }

def  Integer isstaleServerup()  {
        log.info ("started isstaleServerup()")

        //check if stale node is up 
        
        
        def resp_staleNode=
                log.info("stale Node = ${params.stalenode}")
//              try
//                      {
                                 resp_staleNode = shell.exec("sh /cacheDir/glu_scripts/glucheck.sh  ${params.stalenode}")
//                      }
//                              catch (ShellExecException f)
//                                      {       return null
//                                      }

                log.info ("${resp_staleNode}")
                if (  resp_staleNode > 0)
                {
                        log.info ("${params.stalenode} server is up and running")
                        return resp_staleNode
                }
			else
                {
                log.info("stale node is down ")
                return null

                }

        }


def boolean isServerAvail() 
{ 
	log.info ("Started isServerAvail")
        isServerup() == null
	log.info ("Ending isServerAvail")
}
def boolean isStaleServerAvail()
{
	log.info ("started isStaleServerAvail")
	isstaleServerup() == null
}

def portList = ["9701" , "9702", "9703"]

def  checkPortAvailonStaleNode = {

        
        def portAvail=0
		portList.each()
                {
                def servState= shell.exec("sh /cacheDir/glu_scripts/getentryState.sh ${params.stalenode} ${params.servicename} ${it}")
                def entryState= shell.exec("sh /cacheDir/glu_scripts/getservState.sh ${params.stalenode} ${params.servicename} ${it}")
                        if(entryState=='running' && servState=='Started')
                        {
                                //Do nothing
                                portAvail=0
                        }
                        else
                        portAvail = it
                }

                if(portAvail != '0')
                {
                        return portAvail
                }
                else
                return null
		log.info("portAvail=${portAvail}")
}


def  boolean isPortAvailonStaleNode()
{ 
        checkPortAvailonStaleNode() == null
}

def doRetry = {
	log.info ("started Do retry")

	log.info("Started ServRunning passing ${NodeName} ${params.servicename}")
        shell.waitFor(timeout: '30s', heartbeat: '60s'){ duration ->
                        def output= shell.exec(" sh /cacheDir/glu_scripts/load_service_monitor.sh ${NodeName} ${params.service}")
                        log.info "${output}"

                                if(output == 0 )
                                        {
                                                log.warn ("No ${params.service} is running on this server.")
                                                output = 0
                                        }
				else
				{
						log.info("${output} no of services are running")
				}
        //Check for Down service.
//	def RunServ = ServRunning()
	def reqServiceNo = params.reqServiceNo
	log.info(" got output  ${reqServiceNo}") 
        if ( output  < reqServiceNo )
                {
			log.info("starting to shift service to stale node")
                        shiftServ()
                        log.info ("Shifting service to stale node")
                }
        else
        //Do nothing
        log.info("Current running services are with in plan so doing nothing")
	}
}

 def shiftServ = {
		log.info("started shifserv()")
        // This function will shift service
        //1. Check if stale Node is availaible

                def staleNode = params.stalenode

                if(isStaleServerAvail())
                {
                        log.warn ("${staleNode} server is down which is in stale node list now moving to next Node")
                        //shiftNextNode()

                }

                else
                {
                        log.info ("Shifting Service to stale node")
                        shiftstaleNode()
                }

}

 def shiftstaleNode = {

        // This funtion will shift the service to stale node.

        //1. we ll check wheather the port is availaible or not.
	log.info("started shiftstaleNode")
        if(isPortAvailonStaleNode())
        {
                //Port is availaible. We ll shift to this.
//                startServiceonStaleNode()
                log.info("started service on stale node")
        }

        else
        {	startServiceonStaleNode()
                log.info("port on stale isn't free")
        }
}


 def startServiceonStaleNode = {
		log.info("started startServiceStaleNode")

       def  portToRun = checkPortAvailonStaleNode()

        // Got port now starting the service.

        //1. we create a plan using curl and REST api.

      def  plans=shell.exec("sh /cacheDir/glu_scripts/genPlan.sh ${params.stalenode} ${params.servicename} ${portToRun}")

                //2. Execute the plan
        shell.exec(" sh /cacheDir/glu_scripts/execPlan.sh ${plans} ")
                //3. Plan has been executed now. Service should be started now.
                log.info ("${params.servicename} Service has been started on ${params.stalenode} using ${portToRun} port")
}

}


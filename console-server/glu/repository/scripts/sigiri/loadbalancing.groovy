class loadbalancing {
	
def NodeName
def ServiceName

def start = {
	 timers.schedule(timer: loadbalance, repeatFrequency: "15s")
}
	
	def stop = {
		timers.cancel(timer: loadbalance)
	}

def NodeNameList = []

def gen_nodeName = {

	for (i in 1..params.noofserver )
	{
		NodeNameList.add[i] = ${param.datacenter}+ "-sigiri" + i;
		log.info ("$NodeNameList[i]")
		tempNodeName= NodeNameList[i]
		
		if(isServerAvail())
		{
			//Now server is not availaible. Starting load balancing mechanism
			log.info("Server has gone down, Now starting fault tolerance")
			
			doRetry()
			
		}	
	}
}


def loadbalance = {
	//This the main function.
	gen_nodeName() //1. we generate servers list
	
	
}


def boolean ServRunning = { 

	
	shell.waitFor(timeout: '30s', heartbeat: '60s'){ duration ->
			def output= shell.exec(" sh /cacheDir/glu_scripts/load_service_monitor.sh ${NodeName} ${params.service}")
			log.info "${output}"
			
				if(output == 'null')
					{
						log.warn ("No ${params.service} is running on this server.")
						return null
					}
			
	
}

def boolean isServiceDown()
{
	ServRunning() == null
}
	
def isServerup = {

	//check if server is in stale list for this service.
	def DownServList = []
	NodeName='';
	for (i in 1..${params.noofserver}  ) // checking wheather serve/agent is down 
	{
		NodeName=${param.datacenter} + "-sigiri" + i;
		def resp_ssh = shell.exec("ssh ${NodeName} ps -elf|grep -v grep|grep glu")
		if (resp_ssh > 0)
		{
			log.info ("${NodeName} server is up and running")
		}
		else
		{
		DownServList.add(${NodeName}) // add server to a down server list
		log.info ("Server ${NodeName} has been flagged as Down Server ")
		log.info ("Shifting running services to another node.")
				
		}
				
			
					
	}
}


def boolean isServerAvail()
{
	isServerup() == 'null'
}


def checkPortAvail = { 

	portRange=${params.portRange}
	def portAvail=0
		for (int i =1 ; i < 4; i++ )
		{
		servState= shell.exec("sh /cacheDir/glu_scripts/getentryState.sh ${NodeName} ${ServiceName} ${portRange} ${i}")
		entrState= shell.exec("sh /cacheDir/glu_scripts/getservState.sh ${NodeName} ${ServiceName} ${portRange} ${i}")
			if(entryState=='running' && servState=='Started')
			{
				//Do nothing
				portAvail=0
			}
			else
			portAvail=portRange + i
		}
		
		if(portAvail > 0)
		{
			return portAvail
		}
		else
		return null
}


def boolean isPortAvail()
{
	checkPortAvail() == 'null'
}

def doRetry = { 

		
	//Check for Down service.
	
	if ( currentRunningServ() < ${params.reqServiceNo} )
		{
			shiftServ()
			log.info ("Shifting service to stale node")
		}	
	else
	//Do nothing
	log.info("Current running services are with in plan so doing nothing")
}

def shiftServ = { 

	// This function will shift service
	//1. Check if stale Node is availaible
	
		staleNode= ${params.stalenode}
		
		if(isServerAvail())
		{
			log.warn ("${staleNode} server is down which is in stale node list now moving to next Node")
			shiftNextNode()
			
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
	
	if(isPortAvail)
	{
		//Port is availaible. We ll shift to this.
		startService()
		log.info("started service on stale node")
	}
	
	else
	{
		log.info("port on stale isn't free")
	}
}


def startService = { 

	portToRun= checkPortAvail()
	
	// Got port now starting the service. 
	
	//1. we create a plan using curl and REST api. 
	
	plans=shell.exec("sh /cacheDir/glu_scripts/genPlan.sh ${NodeName} ${ServiceName} ${portToRun}")
	
		//2. Execute the plan 
	shell.exec(" sh /cacheDir/glu_scripts/execPlan.sh ${plans} ")
		//3. Plan has been executed now. Service should be started now.
		log.info ("${ServiceName} Service has been started on ${NodeName} using ${portToRun} port")
}

}

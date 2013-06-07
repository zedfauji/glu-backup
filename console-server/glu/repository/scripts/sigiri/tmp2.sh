if  ps aux | grep 'java'|grep -v grep > /dev/null 2>1
then
     echo Running
	exit 0
else
	echo Not-Running
     exit 1
fi

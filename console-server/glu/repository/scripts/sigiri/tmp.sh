if [ `ps -elf|grep -v grep|grep java` ] ; then
echo "process is running"
exit 0
fi



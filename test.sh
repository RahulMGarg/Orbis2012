for i in {1..10}
do
	let K=0
	J=$(sh ./run.sh -ai -speed 30 -nonui)	
	n=$(echo "$J"|sed 's/^.*is: *\([0-9]*\).*$/\1/')
	echo $n
	#K=$K+$Ji

done
#let K=$J/10
#echo $K
#for i in {1..5}
#do
#	let K=2+i
#	sh ./run.sh 32 $K

#	sh ./run.sh 32 $K
#done

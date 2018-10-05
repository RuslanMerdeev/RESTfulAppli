''''''
import os

def create():
	try:
		with open('distances\distances.xml', 'w') as f:
			create_file(f)
	except IOError as err:
		print('IOError: ' + str(err))

def create_file(f):
	print('<?xml version=\"1.0\"?>\n<db>', file=f)
	
	for i in range(10000):
		for j in range(100):
			print('\t<distance>\n\t\t<from_city>' + str(i) + '</from_city>\n\t\t<to_city>' + str(j) + '</to_city>\n\t\t<value>' + str(i) + '</value>\n\t</distance>', file=f)
	
	print('</db>', file=f)

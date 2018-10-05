''''''
import os

def create():
	try:
		with open('cities\cities.xml', 'w') as f:
			create_file(f)
	except IOError as err:
		print('IOError: ' + str(err))

def create_file(f):
	print('<?xml version=\"1.0\"?>\n<db>', file=f)
	
	for i in range(10000):
		print('\t<city>\n\t\t<name>' + str(i) + '</name>\n\t\t<latitude>' + str(i/1000) + '</latitude>\n\t\t<longitude>' + str(i/1000) + '</longitude>\n\t</city>', file=f)
	
	print('</db>', file=f)

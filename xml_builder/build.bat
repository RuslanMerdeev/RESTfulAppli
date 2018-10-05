path "C:\Users\r.merdeev\AppData\Local\Programs\Python\Python36-32"
cd cities
setup.py sdist
setup.py install
pause
cd ..\distances
setup.py sdist
setup.py install
pause
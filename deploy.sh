#!/bin/bash

echo "=== Nettoyage ==="
rm -rf build
rm -f Framework.war

mkdir -p build/classes

echo "=== Compilation ==="

javac \
    -cp "lib/*" \
    -d build/classes \
    $(find src -name "*.java")

if [ $? -ne 0 ]; then
    echo "Compilation failed."
    exit 1
fi

echo "=== Création du WAR ==="

mkdir -p build/war/WEB-INF/classes

cp -r build/classes/* build/war/WEB-INF/classes/
cp -r webapp/WEB-INF build/war/

jar -cf Framework.war -C build/war .

echo "=== Déploiement vers Tomcat 10 ==="

sudo rm -rf /var/lib/tomcat10/webapps/Framework
sudo rm -f /var/lib/tomcat10/webapps/Framework.war

sudo cp Framework.war /var/lib/tomcat10/webapps/

echo "=== Redémarrage Tomcat ==="

sudo systemctl restart tomcat10

echo ""
echo "=== Terminé ==="
echo "Application : http://localhost:8080/Framework/"
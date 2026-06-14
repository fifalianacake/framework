#!/bin/bash
set -e  # stop si erreur (évite 50 bugs en cascade)

APP_NAME="Banque"
SRC_DIR="src/main/java"
WEB_DIR="src/main/webapp"
BUILD_DIR="build"
LIB_DIR="lib"
TOMCAT_WEBAPPS="/media/tojo/Ubuntu/apache-tomcat-9.0.104/webapps"

# ===== JAR nécessaires =====
JDBC_DRIVER_JAR="$LIB_DIR/mysql-connector-j-9.5.0.jar"
SERVLET_API_JAR="$LIB_DIR/javax.servlet-api-4.0.1.jar"
JSON_JAR="$LIB_DIR/json-20250517.jar"

echo "=== Nettoyage ==="
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/WEB-INF/classes"
mkdir -p "$BUILD_DIR/WEB-INF/lib"

echo "=== Compilation Java ==="
find "$SRC_DIR" -name "*.java" > sources.txt

javac -cp "$JDBC_DRIVER_JAR:$SERVLET_API_JAR:$JSON_JAR" \
      -d "$BUILD_DIR/WEB-INF/classes" \
      @sources.txt

rm sources.txt

echo "=== Copie webapp (JSP, HTML, web.xml, etc.) ==="
cp -r "$WEB_DIR"/* "$BUILD_DIR/"

echo "=== Copie librairies runtime ==="
# servlet-api -> fourni par Tomcat (NE PAS copier)
cp "$JDBC_DRIVER_JAR" "$BUILD_DIR/WEB-INF/lib/"
cp "$JSON_JAR" "$BUILD_DIR/WEB-INF/lib/"

echo "=== Création WAR ==="
cd "$BUILD_DIR"
jar -cvf "$APP_NAME.war" *
cd ..

echo "=== Déploiement Tomcat ==="
rm -rf "$TOMCAT_WEBAPPS/$APP_NAME"          # supprime ancienne app déployée
rm -f "$TOMCAT_WEBAPPS/$APP_NAME.war"

cp "$BUILD_DIR/$APP_NAME.war" "$TOMCAT_WEBAPPS/"

echo "=== Déploiement terminé ==="
echo "Si Tomcat tourne déjà, il va auto-déployer. Sinon démarre-le."

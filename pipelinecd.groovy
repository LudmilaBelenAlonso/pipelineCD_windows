pipeline {

    agent {
        label 'nodo'
    }
	
    environment {
        ARTIFACT_PATH = """${ZIP}"""
    }

    stages {
        stage('Descargar desde Nexus') {
            steps {
                script {
                    def APLICACION = """${APLICACION}""".split('-')[0]
                    def NEXUS_URL = """http://servidor/nexus/repository/${APLICACION}/prod/"""
                                        
                    withCredentials([usernamePassword(credentialsId: 'jenkins_nexus', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                        bat "E:\\jenkins\\prueba_pipe\\curl -L -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} ${NEXUS_URL}${ARTIFACT_PATH} -o ${ARTIFACT_PATH}"
                    }
                }
            }
        }

        stage('Descomprimir Archivos') {
            steps {
                script {
                    def archivoZip = "${ARTIFACT_PATH}"

                    //consulta si el archivo existe, si no existe da el mensaje de error, si existe lo descomprime
                    if (!fileExists(archivoZip)) {
                        error "Archivo ZIP no encontrado: ${archivoZip}"
                    } else {
                        bat "unzip -o ${archivoZip} -d ."
                    }

                    //consulta si el archivo existe, entonces borra el .zip, sino informa mendiante mensaje
                    if (fileExists(archivoZip)){
                        bat "del ${archivoZip}"
                    } else {
                        echo 'No existe archivo zip'
                    }
                }
            }
        }

        stage('Copiar a otro servidor Windows') {
            steps {
                script {
                    def failures = []
                    def successful = []
                    def commands = []

                    wrap([$class: 'BuildUser']) {
                        userId = "${BUILD_USER_ID}"
                        user = "${BUILD_USER}"
                        mailuser = "${BUILD_USER_EMAIL}"
                    }

                    Date date = new Date()
                    String fecha = date.format("yyyy-MM-dd-HH-mm")
                    to_email = mailuser
                    
                    //cierra sesiones y libera archivos
                    try {
                        def lista = jsonParse("${APLICACION}")
                        def APP_SESION = APLICACION.split('-')[1]
                        println APP_SESION
                        def JOB_NAME="${JOB_NAME.substring(JOB_NAME.lastIndexOf('/') + 1, JOB_NAME.length())}"

                        if (APP_SESION == 'Sucursal'){
                            lista.each { 
                                def servidor = it.servidor
                                def path = it.path
                                def archivos = it.archivos
                                txts = archivos.replace("[", "").replace("]", "")
                                def listaArchivos = txts.split(' ')

                                for (i = 0; i < listaArchivos.size(); i++) { 
                                  
                                    def infoSesiones = bat(returnStdout: true, script: "qwinsta /server:${servidor}")
                                    println infoSesiones

                                    def lines = infoSesiones.readLines()
                                    def sesionesActivas = []
                        
                                    // Filtrar las líneas que contienen "Active"
                                    lines.each { line ->
                                        if (line.contains('Active')) {
                                            sesionesActivas.add(line)
                                        }
                                    }
                                                                        
                                    println sesionesActivas

                                    if (sesionesActivas != null){ //cierra sesiones solo si hay sesiones en el array
                                        for (i=0 ; i < sesionesActivas.size(); i++){//recorre array de sesiones activas
                                            println sesionesActivas[i]
                                            actSession = sesionesActivas[i].tokenize()//con esa posición genera un nuevo array 
                                            println actSession[2]
                                            actSession_id = actSession[2]
                                            bat "logoff ${actSession_id}"
                                        }
                                    }


                                    def app_activa = bat(script: """TASKLIST /FI "imagename eq ${archivos}"| find /v /c "${archivos}" """, returnStdout: true).tokenize()//convierte en array el comando y resultado, en la posición 10 esta la cantidad de tareas abiertas en formato string                                    
                                    //println app_activa
                                    def app_act_int = app_activa[10].toInteger() //se toma el valor de la posición 10 del array (que contiene la cantidad de procesos abiertos) y lo convierte en entero, se guarda en una variable
                                    
                                    if (app_act_int > 1 ) { //si la variable es mayor a 1 entonces cierra las aplicaciones
                                        bat "TASKKILL /F /IM ${archivos} /T"// termina el proceso y todos los procesos asociados
                                    } 

                                    def ip = servidor
                                    //echo servidor
                                    echo ip

                                    def ruta_path = path 
                                    //echo path
                                    echo ruta_path
                                        
                                    // Encontrar la posición del separador de unidades
                                    def indiceSeparadorUnidad = ruta_path.indexOf(':')
                                    
                                    // Obtener la unidad de la ruta
                                    def unidad = ruta_path.substring(0, indiceSeparadorUnidad + 1)
                                    def unidadPath = unidad.replace(':', '$')
                                    echo unidadPath
                                        
                                    // Obtener el resto de la ruta
                                    def restoPath = ruta_path.substring(indiceSeparadorUnidad + 1)
                                    echo restoPath
                                        
                                    //ruta de workspace
                                    def archivosDescomprimidos = """E:\\jenkins\\workspace\\UC-Tecnologia\\UC-CicloDeCambios\\prueba\\${JOB_NAME}\\"""
                                    echo archivosDescomprimidos

                                    if (ip == 'xfs'){
                                        // Construye la ruta completa en el servidor de destino
                                        def destinoServidor = "\\\\${ip}\\${restoPath}"
                                        echo destinoServidor

                                        //bat "xcopy ${archivosDescomprimidos} ${destinoServidor} /E /Y /I" 
                                        
                                    } 
                                    else { 
                                        // Construye la ruta completa en el servidor de destino
                                        def destinoServidor = "\\\\${ip}\\${unidadPath}${restoPath}"
                                        echo destinoServidor

                                        //bat "xcopy ${archivosDescomprimidos} ${destinoServidor} /E /Y /I" 

                                    }

                                }
                            }
                        } 
                        else {
                            lista.each{
                                def servidor = it.servidor
                                def path = it.path

                                def ip = servidor
                                //echo servidor
                                echo ip

                                def ruta_path = path 
                                //echo path
                                echo ruta_path
                                            
                                // Encontrar la posición del separador de unidades
                                def indiceSeparadorUnidad = ruta_path.indexOf(':')
                                    
                                // Obtener la unidad de la ruta
                                def unidad = ruta_path.substring(0, indiceSeparadorUnidad + 1)
                                def unidadPath = unidad.replace(':', '$')
                                echo unidadPath
                                            
                                // Obtener el resto de la ruta
                                def restoPath = ruta_path.substring(indiceSeparadorUnidad + 1)
                                echo restoPath
                                            
                                // Construye la ruta completa en el servidor de destino
                                def destinoServidor = "\\\\${ip}\\${unidadPath}${restoPath}"
                                echo destinoServidor
                                //ruta de workspace
                                def archivosDescomprimidos = """E:\\jenkins\\workspace\\UC-Tecnologia\\UC-CicloDeCambios\\pruebas\\${JOB_NAME}\\"""
                                //echo archivosDescomprimidos

                                //bat "xcopy ${archivosDescomprimidos} ${destinoServidor} /E /Y /I"  
                                
                            }
                        }

                        //bat "rmdir /s /q ${JOB_NAME}"
                        //bat "mkdir ${JOB_NAME}"  

                        commands.each {
                            def commandResult3 = bat(returnStdout: true, script: it)
                            echo "${it}"
                            successful << commandResult3 + "\n"
                        }
         
                    } 

                    finally{
            
                        echo 'I will always say Hello again!'
                        
                        emailext attachLog: true,//no adjunta el log de la ejecución del pipeline 
                        //attachmentsPattern: '*.txt', //adjunta los files .txt
                        from: '',//remitente del correo
                        to:"",//inserta los mails que se encuentran en la variable para que se los envien
                        body: "Scripts ejecutado: ${env.BUILD_URL}", //cuerpo de email
                        subject: "Resultados: Job ${env.JOB_NAME}" //titulo de email
                    }
                                
                }
            }
        }
    }
}

@NonCPS
def jsonParse(def app) { //acceso a listado en base de datos en phpMyAdmin 
    try {
        def url = "http://servidor:8000/despliegues/listado_despliegues/${app}"
        def postmanGet = new URL("${url}")
        def getConnection = postmanGet.openConnection()
        getConnection.requestMethod = 'GET'
        string = postmanGet.getText()
        echo "${string}"
        return new groovy.json.JsonSlurperClassic().parseText(string)
    } catch (Exception e) {
        println "Error al parsear JSON: ${e.message}"
        return null
    }
}

@NonCPS
def createReport(successful, failures, duration) {
    def message = new StringBuilder()
    message << "<p><h3><strong>Cerrar archivos XFS - ${APLICACION}</strong></h3></p>"
    message << "<p>Operador: ${user}</p>"

    if (duration != '') {
        message << "<p>Duración de la consulta: ${duration}</p>"
    }

    if (!successful.isEmpty()) {
        message << "<p>Pasos Correctos:</p>"
        message << "<ul>"
        message << successful.collect { "<li>${it}</li>" }.join('\n')
        message << "</ul>"
    }

    if (!failures.isEmpty()) {
        message << "<p>Pasos con conflictos:</p>"
        message << "<ul>"
        message << failures.collect { "<li>${it}</li>" }.join('\n')
        message << "</ul>"
    }

    return message.toString()
}

@NonCPS
def clearTmpPath(tmp) {
    def file = new File(tmp)
    if (file.exists()) {
        file.deleteDir()
    }
}
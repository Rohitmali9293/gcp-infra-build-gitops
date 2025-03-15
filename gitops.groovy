pipeline {
    agnet { label 'cm-linux'}
    options {
        timeout (time: 5, unit: 'MINUTES')
        timestamps()
        ansiColor('xtream')
    }
    parameters {}
    eanviroments{}
    stages {
        stage{
            steps{
                script{
                }
            }
        }
    } 
}
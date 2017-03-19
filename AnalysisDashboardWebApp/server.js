// Require node modules
var express = require('express');
var killProcess  = require('tree-kill');
var spawn = require('child_process').spawn;
var sparkIsRunning = false;

// Create global variables for the child processes
var sparkChild;

var app = express();

app.use(express.static(__dirname + '/public'));
app.use("/css", express.static(__dirname + '/css'));
app.use("/styles", express.static(__dirname + '/styles'));
app.use('/js', express.static(__dirname + '/js'));

app.get('/index.htm', function (req, res) {
   res.sendFile( __dirname + "/" + "index.htm" );
});

app.get('/', function (req, res) {
   res.sendFile( __dirname + "/" + "index.htm" );
});

function stopSpark(req, res) {
  if(sparkIsRunning){
    sparkIsRunning = false;
    killProcess(sparkChild.pid);
    console.log("Spark is stopped.");
    res.sendFile( __dirname + "/" + "index.htm" );
  }
  else{
    console.log("### WARNING ###: Prevented an attempt to stop analysis which is not running.");
    res.sendFile( __dirname + "/" + "index.htm" );
  }
};

app.get('/start_spark', function (req, res) {
   if(!sparkIsRunning && req.query.keyword){
       sparkIsRunning = true;
       // Prepare output in JSON format
       console.log("Spark is starting...");

       // Full-path for now - change it later to relative.
       console.log(req.query.keyword);
       var cmd = 'java -cp /home/kirev/git/tweet-analysis/target/twitter-0.0.1-SNAPSHOT.jar com.sparkstreaminganalytics.twitter.TweetsAnalysisPipeline ' + req.query.keyword;

       sparkChild = spawn(cmd, [], {shell: true});

       // If you’re really just passing it through, though, pass {stdio: 'inherit'}
       // to child_process.spawn instead.
       sparkChild.stdout.on ('data', (data) => {
          console.log (data.toString ());
        });

       sparkChild.stderr.on('data', function (data) {
           process.stderr.write(data);
       });
       sparkChild.on('close', function (exitCode) {
          if (exitCode !== 0) {
              console.error(exitCode);
          }
        });
       res.sendFile( __dirname + "/" + "dashboardPage.htm" );
    }
    else{
        if(!req.query.keyword && !sparkIsRunning){
          console.log("### WARNING ###: Prevented an attempt to start analysis with undefined keyword.");
          res.sendFile( __dirname + "/" + "index.htm" );
        }
        else if(!req.query.keyword && sparkIsRunning){
          console.log("### WARNING ###: Prevented an attempt to start analysis with undefined keyword while another analysis is already running.");
          sparkIsRunning = false;
          killProcess(sparkChild.pid);
          console.log("Spark is stopped.");
          res.sendFile( __dirname + "/" + "index.htm" );
        }
        else{
          console.log("### WARNING ###: Prevented an attempt to start analysis while analysis was already running.");
          res.sendFile( __dirname + "/" + "dashboardPage.htm" );
        }  
    }
    
});

app.get('/stop_spark', stopSpark);

var server = app.listen(8081, function () {
   var host = server.address().address
   var port = server.address().port
   console.log("Example app listening at http://%s:%s", host, port)

})
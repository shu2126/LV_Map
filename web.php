<!DOCTYPE html>
<html>
<head>
  <title>CONNECT</title>
  <meta charset="utf-8">
</head>
<body>
  <<?php
//snoopy함수 이용하여 웹 크롤링으로 군산 지역화폐 가맹점 정보 가져오기
include_once 'Snoopy.class.php';
$snoopy = new snoopy;

$conn = mysqli_connect("localhost","root","dkfldfkd100","locallist");
mysqli_set_charset($conn,"utf-8");

for($num = 1;$num<90;$num++){

//크롤링할 사이트 주소 num은 쪽수
  $snoopy->fetch("https://www.gunsan.go.kr/main/m308/list?s_idx=$num");
  $temp=explode("<div class=\"bbslist-td m308\">", $snoopy->results);

    for($i=1;$i<11;$i++){
      $tempjum=explode("</span>", $temp[$i]);

    // name, type, detailaddr, number, addr <- 밑에 각 배열 순서에 저장
    // $tempjum[1], $tempjum[2], $tempjum[3], $tempjum[4], $tempjum[5]
    
    //topic = 테이블 명, locallist = db
    
    $name = explode(">",$tempjum[1]);
    // echo "</br>상호 : ";
    
    $type = explode(">",$tempjum[2]);
    // echo "</br>업종 : ";
    
    $detailaddr = explode(">",$tempjum[3]);
    // echo "</br>상세주소 : ";
    
    $number = explode(">",$tempjum[4]);
    // echo "</br>번호 : ";
    
    $addr = explode(">",$tempjum[5]);
    // echo "</br>행정동 : ";

//웹 크롤링한 정보 db에 저장
    $sql = "insert into topic
      (id, name, type, detailaddr, number, addr)
        VALUES
        (null,'$name[1]','$type[1]', '$detailaddr[1]','$number[1]' ,'$addr[1]'
          )";

    $result = mysqli_query($conn,$sql);
    if($result == false){

      echo mysqli_error($conn);
    }
  }
  echo "</br>$num////////////////////////////////////////////////////////////////////////";
}
   ?>
</body>
</html>


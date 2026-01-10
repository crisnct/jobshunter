package com.jobshunter.service.clients;

import com.jobshunter.dto.IpInfoDetailResponse;
import com.jobshunter.dto.IpInfoResponse;
import com.jobshunter.service.clients.ipinfo.IpInfoClient;
import com.jobshunter.service.testdata.FakeIpInfo;

public sealed interface IpInfo permits IpInfoClient, FakeIpInfo {

  IpInfoResponse getIpDefaultInfo(String ip);

  IpInfoDetailResponse getIpDetailInfo(String ip);

}

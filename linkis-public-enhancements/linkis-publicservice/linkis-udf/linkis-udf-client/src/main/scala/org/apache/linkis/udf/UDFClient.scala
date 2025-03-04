/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.apache.linkis.udf


import org.apache.linkis.rpc.Sender
import org.apache.linkis.udf.api.rpc.{RequestUdfTree, ResponseUdfTree}
import org.apache.linkis.udf.entity.{UDFInfo, UDFTree}
import org.apache.linkis.udf.utils.ConstantVar
import org.apache.linkis.udf.vo.UDFInfoVo
import org.apache.commons.collections.CollectionUtils

import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

object UDFClient {

  @Deprecated
  def getUdfInfos(userName: String, category: String): mutable.ArrayBuffer[UDFInfoVo] = {
    val udfInfoBuilder = new mutable.ArrayBuffer[UDFInfoVo]
    val udfTree = queryUdfRpc(userName, category)
    if (null != udfTree) extractUdfInfos(udfInfoBuilder, udfTree, userName, category)
    udfInfoBuilder
  }

  def getUdfInfosByUdfType(userName: String, category: String, udfType: BigInt): mutable.ArrayBuffer[UDFInfoVo] = {
    val udfInfoBuilder = new mutable.ArrayBuffer[UDFInfoVo]
    val udfTree = queryUdfRpc(userName, category)
    if (null != udfTree) extractUdfInfosByUdfType(udfInfoBuilder, udfTree, userName, category, udfType)
    udfInfoBuilder
  }

  def getJarUdf(userName: String): ArrayBuffer[UDFInfoVo] = {
    getUdfInfosByUdfType(userName, ConstantVar.UDF, ConstantVar.UDF_JAR)
  }

  def getPyUdf(userName: String): ArrayBuffer[UDFInfoVo] = {
    getUdfInfosByUdfType(userName, ConstantVar.UDF, ConstantVar.UDF_PY)
  }

  def getScalaUdf(userName: String): ArrayBuffer[UDFInfoVo] = {
    getUdfInfosByUdfType(userName, ConstantVar.UDF, ConstantVar.UDF_SCALA)
  }

  def getPyFuncUdf(userName: String): ArrayBuffer[UDFInfoVo] = {
    getUdfInfosByUdfType(userName, ConstantVar.FUNCTION, ConstantVar.FUNCTION_PY)
  }

  def getScalaFuncUdf(userName: String): ArrayBuffer[UDFInfoVo] = {
    getUdfInfosByUdfType(userName, ConstantVar.FUNCTION, ConstantVar.FUNCTION_SCALA)
  }

  private def extractUdfInfos(udfInfoBuilder: mutable.ArrayBuffer[UDFInfoVo], udfTree: UDFTree, userName: String, category: String): Unit = {
    if (CollectionUtils.isNotEmpty(udfTree.getUdfInfos)) {
      udfTree.getUdfInfos.foreach { udfInfo: UDFInfoVo =>
        udfInfoBuilder.append(udfInfo)
      }
    }
    if (CollectionUtils.isNotEmpty(udfTree.getChildrens)) {
      udfTree.getChildrens.foreach { child: UDFTree =>
        var childInfo = child
        if (ConstantVar.specialTypes.contains(child.getUserName)) {
          childInfo = queryUdfRpc(userName, category, child.getId, child.getUserName)
        } else {
          childInfo = queryUdfRpc(userName, category, child.getId, ConstantVar.SELF_USER)
        }
        if (null != childInfo) extractUdfInfos(udfInfoBuilder, childInfo, userName, category)
      }
    }
  }

  private def extractUdfInfosByUdfType(udfInfoBuilder: mutable.ArrayBuffer[UDFInfoVo], udfTree: UDFTree,
                                       userName: String, category: String, udfType: BigInt): Unit = {
    if (CollectionUtils.isNotEmpty(udfTree.getUdfInfos)) {
      udfTree.getUdfInfos.filter(infoVo => infoVo.getUdfType == udfType && infoVo.getLoad == true)
        .foreach(infoVo => udfInfoBuilder.append(infoVo))
    }
    if (CollectionUtils.isNotEmpty(udfTree.getChildrens)) {
      udfTree.getChildrens.foreach { child: UDFTree =>
        val childInfo = if (ConstantVar.SYS_USER.equalsIgnoreCase(child.getUserName)) {
          null
        } else if (ConstantVar.specialTypes.contains(child.getUserName)) {
          queryUdfRpc(userName, category, child.getId, child.getUserName)
        } else {
          queryUdfRpc(userName, category, child.getId, ConstantVar.SELF_USER)
        }
        if (null != childInfo) extractUdfInfosByUdfType(udfInfoBuilder, childInfo, userName, category, udfType)
      }
    }
  }

  private def queryUdfRpc(userName: String, category: String, treeId: Long = -1, treeType: String = "self"): UDFTree = {
    val udfTree = Sender.getSender(UDFClientConfiguration.UDF_SERVICE_NAME.getValue)
      .ask(RequestUdfTree(userName, treeType, treeId, category))
      .asInstanceOf[ResponseUdfTree]
      .udfTree
    //info("got udf tree:" + new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(udfTree))
    udfTree
  }
}

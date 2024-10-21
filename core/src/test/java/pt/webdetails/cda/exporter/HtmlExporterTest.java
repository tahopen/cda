/*!
 * Copyright 2002 - 2017 Webdetails, a Hitachi Vantara company. All rights reserved.
 *
 * This software was developed by Webdetails and is provided under the terms
 * of the Mozilla Public License, Version 2.0, or any later version. You may not use
 * this file except in compliance with the license. If you need a copy of the license,
 * please go to  http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. Please refer to
 * the license for the specific language governing your rights and limitations.
 */

package pt.webdetails.cda.exporter;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.swing.table.TableModel;

import org.junit.Test;

import pt.webdetails.cda.test.util.CdaTestHelper;
import pt.webdetails.cpf.Util;

public class HtmlExporterTest {

  @Test
  public void testEmptyHtmlExport() throws Exception {
    TableModel table = BasicExportExamples.getEmptyTable();
    TableExporter exporter = new HtmlExporter( Collections.<String, String>emptyMap() );
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    exporter.export( out, table );
    final String exported = Util.toString( out.toByteArray() );
    final String expected = "<table> <tr/> </table>";
    CdaTestHelper.assertXmlEquals( "html", expected, exported );
  }

  @Test
  public void testNullsHtmlExport() throws Exception {
    TableModel table = BasicExportExamples.getNullOneLiner();
    TableExporter exporter = new HtmlExporter( Collections.<String, String>emptyMap() );
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    exporter.export( out, table );
    final String exported = Util.toString( out.toByteArray() );
    final String expected = "<table>"
      + "<tr>"
      + "  <th>long null</th> <th>string null</th> <th>double null</th>"
      + "  <th>date null</th> <th>big decimal null</th> "
      + "</tr>"
      + "<tr>"
      + "  <td>#NULL</td> <td>#NULL</td> <td>#NULL</td> <td>#NULL</td> <td>#NULL</td>"
      + "</tr>"
      + "</table>";
    CdaTestHelper.assertXmlEquals( "html", expected, exported );
  }

  @Test
  public void testHtmlExport1() throws Exception {

    TableModel table = BasicExportExamples.getTestTable1();

    Map<String, String> settings = new HashMap<>( 0 );
    final TimeZone tz = TimeZone.getDefault();
    try {
      TimeZone.setDefault( TimeZone.getTimeZone( "GMT" ) );
      TableExporter exporter = new HtmlExporter( settings );
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      exporter.export( out, table );
      final String exported = Util.toString( out.toByteArray() );

      final String expected =
        "<table>\n"
          + "  <tr>\n"
          + "    <th>The Integer</th>\n"
          + "    <th>The String</th>\n"
          + "    <th>The Numeric</th>\n"
          + "    <th>The Date</th>\n"
          + "    <th>The Calculation</th>\n"
          + "  </tr>\n"
          + "  <tr>\n"
          + "    <td>1</td>\n"
          + "    <td>One</td>\n"
          + "    <td>1.05</td>\n"
          + "    <td>2012-01-01T00:01:01.000+0000</td>\n"
          + "    <td>-12.34567890123456789</td>\n"
          + "  </tr>\n"
          + "  <tr>\n"
          + "    <td>-2</td>\n"
          + "    <td>Two &gt; One</td>\n"
          + "    <td>-1.05</td>\n"
          + "    <td>#NULL</td>\n"
          + "    <td>987654321.12345678900</td>\n"
          + "  </tr>\n"
          + "  <tr>\n"
          + "    <td>9223372036854775807</td>\n"
          + "    <td>Many</td>\n"
          + "    <td>1.7976931348623157E308</td>\n"
          + "    <td>1970-01-01T00:00:00.000+0000</td>\n"
          + "    <td>4.9E-325</td>\n"
          + "  </tr>\n"
          + "</table>";

      CdaTestHelper.assertXmlEquals( "html", expected, exported );
    } finally {
      TimeZone.setDefault( tz );
    }
  }

}

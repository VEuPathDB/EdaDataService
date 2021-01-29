#!groovy

@Library('pipelib')
import org.veupathdb.lib.Builder

node('centos8') {

  checkout scm

  def builder = new Builder(this)
  builder.buildContainers([[name: 'eda-data']])

}

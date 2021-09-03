#!groovy

@Library('pipelib')
import org.veupathdb.lib.Builder

node('centos8') {

  builder.gitClone()

  def builder = new Builder(this)
  builder.buildContainers([[name: 'eda-data']])

}
